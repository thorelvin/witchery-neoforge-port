package com.emoniph.witchery.world;

import com.emoniph.witchery.blocks.TormentPortalBlock;
import com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates the Torment dimension as six stacked DFS maze levels.
 *
 * The layout follows old Witchery's GenerateMaze display path:
 * - origin X=-23, Z=6
 * - player entry near (8, y+2, 8)
 * - portal near (8, y+1..2, 68)
 *
 * Vertical passage layout:
 * - y - 1: Bedrock floor
 * - y: Bedrock, with rare Mycelium
 * - y + 1..4: Air
 * - y + 5: Force ceiling
 *
 * Walls are six-block-tall Force columns. Force is intentionally invisible and
 * light-transparent like the old BlockForce, but remains solid for collision.
 */
public class TormentChunkGenerator extends ChunkGenerator {

    public static final MapCodec<TormentChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(inst, TormentChunkGenerator::new));

    public static final int NUM_LEVELS   = TormentHelper.NUM_LEVELS;
    public static final int BASE_LEVEL   = TormentHelper.BASE_LEVEL;
    public static final int LEVEL_HEIGHT = TormentHelper.LEVEL_HEIGHT;
    public static final int MAZE_SIZE    = TormentHelper.MAZE_SIZE;

    private static final int WALL_HEIGHT = 6;
    private static final int ORIGIN_X = TormentHelper.ORIGIN_X;
    private static final int ORIGIN_Z = TormentHelper.ORIGIN_Z;
    private static final int STRUCTURE_MIN_X = TormentHelper.STRUCTURE_MIN_X;
    private static final int STRUCTURE_MAX_X = TormentHelper.STRUCTURE_MAX_X;
    private static final int STRUCTURE_MIN_Z = TormentHelper.STRUCTURE_MIN_Z;
    private static final int STRUCTURE_MAX_Z = TormentHelper.STRUCTURE_MAX_Z;

    // Old GenerateMaze direction encoding: N=1, S=2, E=4, W=8.
    private static final int N = 1, S = 2, E = 4, W = 8;
    private static final int[] DX = {0, 0, 1, -1};
    private static final int[] DZ = {-1, 1, 0, 0};
    private static final int[] BITS = {N, S, E, W};
    private static final int[] OPP  = {S, N, W, E};

    /** Cached mazes keyed by world seed. [level][mx][mz] = direction bitmask. */
    private static final ConcurrentHashMap<Long, int[][][]> MAZE_CACHE = new ConcurrentHashMap<>();

    public TormentChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState random, ChunkAccess chunk) {
        if (!intersectsMazeFootprint(chunk)) {
            return;
        }

        long seed = region.getSeed();
        int[][][] mazes = getMazes(seed);
        BlockState force = WitcheryBlocks.FORCE.get().defaultBlockState();
        BlockState floor = Blocks.BEDROCK.defaultBlockState();
        BlockState portal = WitcheryBlocks.TORMENT_PORTAL.get().defaultBlockState()
                .setValue(TormentPortalBlock.AXIS, Direction.Axis.X);
        BlockState chest = WitcheryBlocks.REFILLING_CHEST.get().defaultBlockState();
        BlockState mycelium = Blocks.MYCELIUM.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int level = 0; level < NUM_LEVELS; level++) {
            drawLegacyMazeLevel(chunk, seed, level, mazes[level], force, floor, portal, chest, mycelium, air);
        }
    }

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
        Collections.shuffle(Arrays.asList(dirs), rng);

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

    private static boolean intersectsMazeFootprint(ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        return maxX >= STRUCTURE_MIN_X && minX <= STRUCTURE_MAX_X
                && maxZ >= STRUCTURE_MIN_Z && minZ <= STRUCTURE_MAX_Z;
    }

    private static void drawLegacyMazeLevel(ChunkAccess chunk, long seed, int level, int[][] maze,
                                            BlockState walls, BlockState floor, BlockState portal,
                                            BlockState chest, BlockState mycelium, BlockState air) {
        int origX = ORIGIN_X;
        int origY = BASE_LEVEL + level * LEVEL_HEIGHT;
        int origZ = ORIGIN_Z;
        int i;

        for (i = 0; i < MAZE_SIZE; i++) {
            int j;
            for (j = 0; j < MAZE_SIZE; j++) {
                drawWall(chunk, origX + j * 2, origY, origZ + 2 * i, walls);
                if ((maze[j][i] & N) == 0) {
                    drawWall(chunk, origX + j * 2 + 1, origY, origZ + 2 * i, walls);
                } else {
                    drawPassage(chunk, seed, level, origX + j * 2 + 1, origY,
                            origZ + 2 * i, walls, floor, mycelium, air);
                }
            }

            drawWall(chunk, origX + j * 2, origY, origZ + 2 * i, walls);

            for (j = 0; j < MAZE_SIZE; j++) {
                if ((maze[j][i] & W) == 0) {
                    drawWall(chunk, origX + j * 2, origY, origZ + 2 * i + 1, walls);
                } else {
                    drawPassage(chunk, seed, level, origX + j * 2, origY,
                            origZ + 2 * i + 1, walls, floor, mycelium, air);
                }
                drawPassage(chunk, seed, level, origX + j * 2 + 1, origY,
                        origZ + 2 * i + 1, walls, floor, mycelium, air);
            }

            drawWall(chunk, origX + j * 2, origY, origZ + 2 * i + 1, walls);
        }

        int j;
        for (j = 0; j < MAZE_SIZE; j++) {
            drawWall(chunk, origX + j * 2, origY, origZ + 2 * i, walls);
            drawWall(chunk, origX + j * 2 + 1, origY, origZ + 2 * i, walls);
        }
        drawWall(chunk, origX + j * 2, origY, origZ + 2 * i, walls);

        drawPortalChambersAndChests(chunk, seed, level, walls, floor, portal, chest, mycelium, air);
    }

    private static void drawPortalChambersAndChests(ChunkAccess chunk, long seed, int level,
                                                    BlockState walls, BlockState floor, BlockState portal,
                                                    BlockState chest, BlockState mycelium, BlockState air) {
        int origX = ORIGIN_X;
        int origY = BASE_LEVEL + level * LEVEL_HEIGHT;
        int origZ = ORIGIN_Z;

        int chamberWidth = 7;
        int chamberHalf = chamberWidth / 2;
        for (int x = 0; x < chamberWidth; x++) {
            for (int z = 0; z < chamberWidth; z++) {
                drawPassage(chunk, seed, level, origX + MAZE_SIZE + x - chamberHalf, origY,
                        origZ + z + 1, walls, floor, mycelium, air);
            }
        }

        for (int x = 0; x < chamberWidth; x++) {
            for (int z = 0; z < chamberWidth + 2; z++) {
                drawPassage(chunk, seed, level, origX + MAZE_SIZE + x - chamberHalf, origY,
                        origZ + 2 * MAZE_SIZE + z - chamberWidth, walls, floor, mycelium, air);
            }
        }

        drawPortal(chunk, origX + MAZE_SIZE, origY, origZ + 2 * MAZE_SIZE, portal, floor);

        int sideRoomWidth = 5;
        int sideRoomHalf = sideRoomWidth / 2;
        int shift = chestShift(seed, level, 0);
        for (int x = 0; x < sideRoomWidth; x++) {
            for (int z = 0; z < sideRoomWidth; z++) {
                drawPassage(chunk, seed, level, origX + x + 1, origY,
                        origZ + z + MAZE_SIZE - sideRoomHalf + shift, walls, floor, mycelium, air);
            }
        }
        drawChest(chunk, origX + sideRoomHalf + 1, origY, origZ + MAZE_SIZE + shift, chest);

        shift = chestShift(seed, level, 1);
        for (int x = 0; x < sideRoomWidth; x++) {
            for (int z = 0; z < sideRoomWidth; z++) {
                drawPassage(chunk, seed, level, origX + 2 * MAZE_SIZE + x - sideRoomWidth, origY,
                        origZ + z + MAZE_SIZE - sideRoomHalf + shift, walls, floor, mycelium, air);
            }
        }
        drawChest(chunk, origX + 2 * MAZE_SIZE - sideRoomHalf - 1, origY, origZ + MAZE_SIZE + shift, chest);

        int roomWidth = 7;
        int roomHalf = roomWidth / 2;
        for (int x = 0; x < roomWidth; x++) {
            for (int z = 0; z < roomWidth; z++) {
                drawPassage(chunk, seed, level, origX + MAZE_SIZE + x - roomHalf, origY,
                        origZ + MAZE_SIZE + z - roomHalf, walls, floor, mycelium, air);
            }
        }
        drawChest(chunk, origX + MAZE_SIZE, origY, origZ + MAZE_SIZE, chest);
    }

    private static void drawPortal(ChunkAccess chunk, int x, int y, int z,
                                   BlockState portal, BlockState floor) {
        setBlock(chunk, x, y + 1, z, portal);
        setBlock(chunk, x, y + 2, z, portal);
        setBlock(chunk, x, y + 3, z, floor);
        for (int side : new int[]{-1, 1}) {
            setBlock(chunk, x + side, y + 1, z, floor);
            setBlock(chunk, x + side, y + 2, z, floor);
            setBlock(chunk, x + side, y + 3, z, floor);
        }
    }

    private static void drawChest(ChunkAccess chunk, int x, int y, int z, BlockState chest) {
        BlockPos pos = new BlockPos(x, y, z);
        if (setBlock(chunk, pos, chest)) {
            chunk.setBlockEntity(new RefillingChestBlockEntity(pos, chest));
        }
    }

    private static void drawWall(ChunkAccess chunk, int x, int y, int z, BlockState wallBlock) {
        for (int h = 0; h < WALL_HEIGHT; h++) {
            setBlock(chunk, x, y + h, z, wallBlock);
        }
    }

    private static void drawPassage(ChunkAccess chunk, long seed, int level, int x, int y, int z,
                                    BlockState wallBlock, BlockState floorBlock,
                                    BlockState mycelium, BlockState air) {
        setBlock(chunk, x, y - 1, z, floorBlock);
        setBlock(chunk, x, y, z, myceliumOrFloor(x, z, seed, level, floorBlock, mycelium));

        for (int h = 1; h < WALL_HEIGHT - 1; h++) {
            setBlock(chunk, x, y + h, z, air);
        }

        setBlock(chunk, x, y + WALL_HEIGHT - 1, z, wallBlock);
    }

    private static BlockState myceliumOrFloor(int wx, int wz, long seed, int level,
                                              BlockState floor, BlockState mycelium) {
        long h = seed ^ ((long) wx * 0x9e3779b97f4a7c15L) ^ ((long) wz * 0x6c62272e07bb0142L)
                ^ ((long) level * 0x517cc1b727220a95L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return Math.floorMod(h, 100L) == 0 ? mycelium : floor;
    }

    private static int chestShift(long seed, int level, int index) {
        long h = seed ^ ((long) level * 0x632be59bd9b4e019L) ^ ((long) index * 0x85157af5L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (int) Math.floorMod(h, 11L) - 5;
    }

    private static boolean setBlock(ChunkAccess chunk, int wx, int wy, int wz, BlockState state) {
        return setBlock(chunk, new BlockPos(wx, wy, wz), state);
    }

    private static boolean setBlock(ChunkAccess chunk, BlockPos pos, BlockState state) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        if (pos.getY() >= 0 && pos.getY() < 256
                && pos.getX() >= minX && pos.getX() < minX + 16
                && pos.getZ() >= minZ && pos.getZ() < minZ + 16) {
            chunk.setBlockState(pos, state, 0);
            return true;
        }
        return false;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                             BiomeManager biomeManager, StructureManager structures,
                             ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor level, RandomState random) {
        return BASE_LEVEL + (NUM_LEVELS - 1) * LEVEL_HEIGHT + WALL_HEIGHT;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] col = new BlockState[level.getHeight()];
        Arrays.fill(col, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(level.getMinY(), col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add(String.format("Torment cell (%d, %d)", (pos.getX() - ORIGIN_X) / 2, (pos.getZ() - ORIGIN_Z) / 2));
    }
}
