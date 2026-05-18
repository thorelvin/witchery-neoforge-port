package com.emoniph.witchery.world;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates the Torment dimension as six stacked DFS maze levels.
 *
 * All chunks are air by default.
 * Each maze level (0-5) is centered at X=0, Z=0 (spans world blocks 0-62 in X and Z).
 * Maze Y:     mazeY   = BASE_LEVEL + level * LEVEL_HEIGHT  (10, 25, 40, 55, 70, 85)
 * Entry Y:    entryY  = mazeY + 2
 *
 * Vertical block layout per maze position:
 *   mazeY - 1          → Torment Stone (floor)
 *   mazeY              → Torment Stone (1 % Mycelium) if passage, Force if wall
 *   mazeY + 1..4       → Air if passage, Force if wall
 *   mazeY + 5          → Force (ceiling, always)
 *
 * The maze is a perfect 31×31 DFS maze generated deterministically from world seed.
 * A Torment Portal is placed at cell (30, 30) in each level (world X=61, Z=61).
 */
public class TormentChunkGenerator extends ChunkGenerator {

    public static final MapCodec<TormentChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(inst, TormentChunkGenerator::new));

    // ── Maze constants ────────────────────────────────────────────────────────

    public static final int NUM_LEVELS   = 6;
    public static final int BASE_LEVEL   = 10;
    public static final int LEVEL_HEIGHT = 15;
    public static final int MAZE_SIZE    = 31;

    // Maze footprint: world X/Z 0..62 (cells at odd positions 1,3,..61; walls at even)
    private static final int MAZE_MAX_COORD = 62;

    // DFS direction encoding: N=1, E=2, S=4, W=8
    private static final int N = 1, E = 2, S = 4, W = 8;
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DZ = {-1, 0, 1, 0};
    private static final int[] BITS = {N, E, S, W};
    private static final int[] OPP  = {S, W, N, E};

    /** Cached mazes keyed by world seed. [level][mx][mz] = direction bitmask. */
    private static final ConcurrentHashMap<Long, int[][][]> MAZE_CACHE = new ConcurrentHashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public TormentChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    // ── fillFromNoise: solid-air pass (no seed needed) ────────────────────────

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        // Torment is air by default — maze is carved in buildSurface where seed is available
        return CompletableFuture.completedFuture(chunk);
    }

    // ── buildSurface: maze placement (world seed available) ───────────────────

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState random, ChunkAccess chunk) {
        long seed = region.getSeed();
        int[][][] mazes = getMazes(seed);

        int bx = chunk.getPos().getMinBlockX();
        int bz = chunk.getPos().getMinBlockZ();

        BlockState force        = WitcheryBlocks.FORCE.get().defaultBlockState();
        BlockState tormentStone = WitcheryBlocks.TORMENT_STONE.get().defaultBlockState();
        BlockState portal       = WitcheryBlocks.TORMENT_PORTAL.get().defaultBlockState();
        BlockState mycelium     = Blocks.MYCELIUM.defaultBlockState();
        BlockState air          = Blocks.AIR.defaultBlockState();

        for (int lx = 0; lx < 16; lx++) {
            int wx = bx + lx;
            if (wx < 0 || wx > MAZE_MAX_COORD) continue;

            for (int lz2 = 0; lz2 < 16; lz2++) {
                int wz = bz + lz2;
                if (wz < 0 || wz > MAZE_MAX_COORD) continue;

                for (int level = 0; level < NUM_LEVELS; level++) {
                    int mazeY = BASE_LEVEL + level * LEVEL_HEIGHT;
                    boolean passage = isPassage(wx, wz, mazes[level]);

                    // Floor
                    setBlock(chunk, wx, mazeY - 1, wz, tormentStone);

                    // Passage or wall columns
                    for (int y = mazeY; y <= mazeY + 5; y++) {
                        if (y == mazeY + 5) {
                            // Ceiling always Force
                            setBlock(chunk, wx, y, wz, force);
                        } else if (passage) {
                            if (y == mazeY) {
                                // 1 % mycelium on floor level
                                setBlock(chunk, wx, y, wz, myceliumOrStone(wx, wz, seed, level, tormentStone, mycelium));
                            } else {
                                setBlock(chunk, wx, y, wz, air);
                            }
                        } else {
                            setBlock(chunk, wx, y, wz, force);
                        }
                    }

                    // Torment Portal at far corner cell (30, 30) → world (61, 61)
                    if (wx == 61 && wz == 61) {
                        setBlock(chunk, wx, mazeY + 1, wz, portal);
                        setBlock(chunk, wx, mazeY + 2, wz, portal);
                    }

                    // Refilling Chests: central cell (15,15)→world(31,31), side cells (5,15)→(11,31) and (25,15)→(51,31)
                    BlockState chest = WitcheryBlocks.REFILLING_CHEST.get().defaultBlockState();
                    if ((wx == 31 && wz == 31) || (wx == 11 && wz == 31) || (wx == 51 && wz == 31)) {
                        if (passage) setBlock(chunk, wx, mazeY + 1, wz, chest);
                    }
                }
            }
        }
    }

    // ── Maze access ───────────────────────────────────────────────────────────

    private static int[][][] getMazes(long seed) {
        return MAZE_CACHE.computeIfAbsent(seed, TormentChunkGenerator::generateAllMazes);
    }

    private static int[][][] generateAllMazes(long seed) {
        int[][][] all = new int[NUM_LEVELS][MAZE_SIZE][MAZE_SIZE];
        for (int level = 0; level < NUM_LEVELS; level++) {
            Random rng = new Random(seed ^ ((long) level * 0x9e3779b97f4a7c15L));
            boolean[][] visited = new boolean[MAZE_SIZE][MAZE_SIZE];
            dfs(0, 0, all[level], visited, rng);
        }
        return all;
    }

    private static void dfs(int mx, int mz, int[][] maze, boolean[][] visited, Random rng) {
        visited[mx][mz] = true;
        Integer[] dirs = {0, 1, 2, 3};
        // Fisher-Yates shuffle
        for (int i = 3; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = tmp;
        }
        for (int d : dirs) {
            int nx = mx + DX[d];
            int nz = mz + DZ[d];
            if (nx >= 0 && nx < MAZE_SIZE && nz >= 0 && nz < MAZE_SIZE && !visited[nx][nz]) {
                maze[mx][mz] |= BITS[d];
                maze[nx][nz] |= OPP[d];
                dfs(nx, nz, maze, visited, rng);
            }
        }
    }

    /**
     * Returns true if world position (wx, wz) is a passage (open air) rather than a wall.
     * Maze occupies wx/wz 0..62. Outer edges (0, 62) are always walls.
     * Cells are at odd positions (1,3,5,...,61) in both axes.
     * Wall slots between cells are at even positions (2,4,...,60).
     */
    private static boolean isPassage(int wx, int wz, int[][] maze) {
        if (wx == 0 || wx == MAZE_MAX_COORD || wz == 0 || wz == MAZE_MAX_COORD) return false;

        int lx = wx - 1; // 0-based: cells at 0,2,4,... walls at 1,3,5,...
        int lz = wz - 1;
        boolean lxOdd = (lx & 1) == 1;
        boolean lzOdd = (lz & 1) == 1;

        if (!lxOdd && !lzOdd) return true;  // cell centre
        if (lxOdd && lzOdd)  return false;  // corner

        if (lxOdd) {
            // E-W wall between cell (lx>>1, lz>>1) and (lx>>1+1, lz>>1)
            int cx = lx >> 1;
            int cz = lz >> 1;
            return cx >= 0 && cx < MAZE_SIZE - 1 && cz >= 0 && cz < MAZE_SIZE
                    && (maze[cx][cz] & E) != 0;
        } else {
            // N-S wall between cell (lx>>1, lz>>1) and (lx>>1, lz>>1+1)
            int cx = lx >> 1;
            int cz = lz >> 1;
            return cx >= 0 && cx < MAZE_SIZE && cz >= 0 && cz < MAZE_SIZE - 1
                    && (maze[cx][cz] & S) != 0;
        }
    }

    private static BlockState myceliumOrStone(int wx, int wz, long seed, int level,
                                               BlockState stone, BlockState myc) {
        long h = seed ^ ((long) wx * 0x9e3779b97f4a7c15L) ^ ((long) wz * 0x6c62272e07bb0142L)
                ^ ((long) level * 0x517cc1b727220a95L);
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
        return (h & 0x7FL) == 0 ? myc : stone; // ~1/128 ≈ 0.8 %
    }

    private static void setBlock(ChunkAccess chunk, int wx, int wy, int wz, BlockState state) {
        if (wy >= 0 && wy < 256) {
            chunk.setBlockState(new BlockPos(wx, wy, wz), state, 0);
        }
    }

    // ── Abstract stubs ────────────────────────────────────────────────────────

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                             BiomeManager biomeManager, StructureManager structures,
                             ChunkAccess chunk) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public int getGenDepth()  { return 256; }

    @Override
    public int getMinY()      { return 0; }

    @Override
    public int getSeaLevel()  { return 63; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor level, RandomState random) {
        return BASE_LEVEL + (NUM_LEVELS - 1) * LEVEL_HEIGHT + 6;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] col = new BlockState[level.getHeight()];
        Arrays.fill(col, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(level.getMinY(), col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add(String.format("Torment cell (%d, %d)", (pos.getX() - 1) / 2, (pos.getZ() - 1) / 2));
    }
}
