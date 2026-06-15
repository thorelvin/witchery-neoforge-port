package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates adaptive fortress walls around a vanilla village.
 * Source parity: WitcheryWalls 1.12.2 VillageWallGenerator, ported to 1.21.11.
 *
 * Algorithm:
 *  1. Collect all village piece bounding boxes and find extents.
 *  2. Build a 2D occupancy grid (1 cell = 1 world block).
 *  3. Fill narrow gaps between structures (range-7 pass).
 *  4. Identify perimeter cells (empty, adjacent to interior).
 *  5. Detect gate positions by scanning for DIRT_PATH blocks.
 *  6. Place cobblestone walls 8 high with merlon caps on perimeter cells.
 *  7. Carve 5-wide gate arches at gate positions.
 *  8. Spawn guards on wall-top every ~150 usable wall-walk blocks.
 */
public final class VillageWallGenerator {
    public record GenerationResult(boolean generated, int gateCandidates, int gates) {
        public static GenerationResult skipped(int gateCandidates, int gates) {
            return new GenerationResult(false, gateCandidates, gates);
        }
    }

    private record GateDetection(List<GateDef> gates, int candidates) {}

    private record WallColumnResult(boolean placed, BlockPos guardPos) {
        private static final WallColumnResult SKIPPED = new WallColumnResult(false, null);
    }

    private static final int WALL_HEIGHT   = 6;   // blocks above surface
    private static final int GATEHOUSE_CLEAR_ABOVE_WALL = 10;
    private static final int MIN_BUILD_Y   = -64;
    private static final int MAX_BUILD_Y   = 319;
    // PADDING must exceed SMOOTH + BLOCK (see step 3c) so the coarse-hull blocks can never
    // reach the grid border; if the INTERIOR touches the edge the flood-fill cannot mark
    // OUTSIDE there and that side is left unwalled.
    private static final int PADDING       = 18;  // extra space outside village edge
    private static final int MAX_GRID      = 260; // abort if village is too large
    private static final int GAP_FILL      = 4;   // max gap to bridge between structures
    private static final int GUARD_SPACING = 150; // usable wall-walk blocks between guard spawns
    private static final int GATE_FLAT_FACE_HALF_WIDTH = 1; // path must hit a 3-wide flat wall face
    private static final int GATE_PATH_OUTWARD_MAX = 10;
    private static final int GATE_PATH_INWARD_MAX = 15;
    private static final int GATEHOUSE_HALF_WIDTH = 3;
    private static final int TREE_CLEAR_MAX_BLOCKS = 384;
    private static final int TREE_CLEAR_SCAN_Y_ABOVE = 24;
    private static final int TREE_CLEAR_SCAN_Y_BELOW = 2;
    private static final int TREE_CLEAR_WALL_TOUCH_RADIUS = 1;
    private static final int TREE_CLEAR_PATH_TOUCH_RADIUS = 1;
    private static final int TREE_CLEAR_TREE_RADIUS = 7;
    private static final int WALL_ENTITY_RELOCATION_RADIUS = 12;
    // Morphological dilation applied to the village footprint before perimeter detection.
    // Expands INTERIOR outward by this many cells, absorbing small concave recesses so
    // the wall traces a smoother, more regularised contour (see reference: contour
    // regularisation / morphological closing).  Gate detection depth is widened to match.
    private static final int SMOOTH        = 4;

    // Grid cell values
    private static final byte EMPTY    = 0;
    private static final byte INTERIOR = 1;
    private static final byte WALL     = 2;
    private static final byte GATE     = 3;

    private VillageWallGenerator() {}

    // ── Entry point ────────────────────────────────────────────────────────────

    public static GenerationResult generate(ServerLevel level, StructureStart start) {
        List<StructurePiece> pieces = start.getPieces();
        WitcheryMod.LOGGER.info("[VillageWalls] generate() called: {} pieces, bb={}",
                pieces.size(), start.getBoundingBox());
        if (pieces.isEmpty()) return GenerationResult.skipped(0, 0);
        boolean spawnGuards = !isAbandonedVillage(start);

        // 1. Village extents
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (StructurePiece p : pieces) {
            BoundingBox b = p.getBoundingBox();
            minX = Math.min(minX, b.minX()); minZ = Math.min(minZ, b.minZ());
            maxX = Math.max(maxX, b.maxX()); maxZ = Math.max(maxZ, b.maxZ());
        }

        int originX = minX - PADDING;
        int originZ = minZ - PADDING;
        int w = (maxX - minX) + PADDING * 2 + 1;
        int d = (maxZ - minZ) + PADDING * 2 + 1;

        if (w > MAX_GRID || d > MAX_GRID) {
            WitcheryMod.LOGGER.info("[VillageWalls] Village too large ({}x{}), skipping.", w, d);
            return GenerationResult.skipped(0, 0);
        }

        byte[][] grid = new byte[w][d];

        // 2. Mark piece footprints as INTERIOR
        for (StructurePiece p : pieces) {
            BoundingBox b = p.getBoundingBox();
            int x1 = b.minX() - originX, z1 = b.minZ() - originZ;
            int x2 = b.maxX() - originX, z2 = b.maxZ() - originZ;
            for (int gx = Math.max(0, x1); gx <= Math.min(w-1, x2); gx++)
                for (int gz = Math.max(0, z1); gz <= Math.min(d-1, z2); gz++)
                    grid[gx][gz] = INTERIOR;
        }

        // 3. Gap fill: bridge short gaps between structure groups
        for (int x = 0; x < w - GAP_FILL; x++) {
            for (int z = 0; z < d - GAP_FILL; z++) {
                if (grid[x][z] != INTERIOR) continue;
                for (int p = 1; p < GAP_FILL; p++) {
                    // East bridge
                    if (grid[x+p][z] == INTERIOR && grid[x+p-1][z] == EMPTY)
                        for (int f = 1; f < p; f++) grid[x+f][z] = INTERIOR;
                    // South bridge
                    if (grid[x][z+p] == INTERIOR && grid[x][z+p-1] == EMPTY)
                        for (int f = 1; f < p; f++) grid[x][z+f] = INTERIOR;
                }
            }
        }

        // 3b. Morphological dilation: expand INTERIOR outward by SMOOTH cells.
        //     Each pass absorbs any EMPTY cell adjacent to INTERIOR, filling concave
        //     recesses. The perimeter is then computed on the smoother region.
        for (int s = 0; s < SMOOTH; s++) {
            byte[][] snap = copyGrid(grid, w, d);
            for (int x = 1; x < w - 1; x++) {
                for (int z = 1; z < d - 1; z++) {
                    if (snap[x][z] != EMPTY) continue;
                    if (snap[x-1][z] == INTERIOR || snap[x+1][z] == INTERIOR ||
                        snap[x][z-1] == INTERIOR || snap[x][z+1] == INTERIOR)
                        grid[x][z] = INTERIOR;
                }
            }
        }

        // 3c. COARSE orthogonal convex hull: quantise the footprint into BLOCK-sized cells,
        //     then make that coarse grid row-convex AND column-convex.  Working at coarse
        //     resolution means the boundary is built from long straight runs joined by big
        //     90° corners ("several square boxes") instead of a fine 1-cell staircase that
        //     reads as a 45° line and forces thick terraced walls.  Row+column convexity
        //     also removes concave pockets along both axes, so the wall only follows the
        //     outer outline.
        final int BLOCK = 8;
        int cw = (w + BLOCK - 1) / BLOCK;
        int cd = (d + BLOCK - 1) / BLOCK;
        boolean[][] coarse = new boolean[cw][cd];
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++)
                if (grid[x][z] == INTERIOR) coarse[x / BLOCK][z / BLOCK] = true;

        // Orthogonal convex hull on the coarse grid (row-convex + column-convex)
        int[] cMinX = new int[cd], cMaxX = new int[cd];
        int[] cMinZ = new int[cw], cMaxZ = new int[cw];
        java.util.Arrays.fill(cMinX, cw); java.util.Arrays.fill(cMaxX, -1);
        java.util.Arrays.fill(cMinZ, cd); java.util.Arrays.fill(cMaxZ, -1);
        for (int cx = 0; cx < cw; cx++)
            for (int cz = 0; cz < cd; cz++)
                if (coarse[cx][cz]) {
                    cMinX[cz] = Math.min(cMinX[cz], cx); cMaxX[cz] = Math.max(cMaxX[cz], cx);
                    cMinZ[cx] = Math.min(cMinZ[cx], cz); cMaxZ[cx] = Math.max(cMaxZ[cx], cz);
                }
        for (int cz = 0; cz < cd; cz++)
            if (cMaxX[cz] >= 0)
                for (int cx = cMinX[cz]; cx <= cMaxX[cz]; cx++) coarse[cx][cz] = true;
        for (int cx = 0; cx < cw; cx++)
            if (cMaxZ[cx] >= 0)
                for (int cz = cMinZ[cx]; cz <= cMaxZ[cx]; cz++) coarse[cx][cz] = true;

        // Map the coarse hull back onto the fine grid
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++)
                if (coarse[x / BLOCK][z / BLOCK] && grid[x][z] == EMPTY)
                    grid[x][z] = INTERIOR;

        // NOTE: no interior-corner filling here.  Filling diagonal INTERIOR notches
        // cascades — each fill exposes a new diagonal notch — and zippers the entire
        // orthogonal-convex-hull silhouette into its bounding box (a plain square).
        // The hull's staircase boundary (the desired "several 90° turns") must be kept.
        // Diagonal WALL connectivity is handled after perimeter detection instead (step 5b).

        // 4. Flood-fill from grid border to mark cells truly OUTSIDE the village.
        //    Cells not reachable from the border are enclosed "holes" (internal paths,
        //    plazas) and must NOT become wall cells — only the outer ring should be walled.
        byte OUTSIDE = 4;
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            if (grid[x][0]   == EMPTY) { grid[x][0]   = OUTSIDE; queue.add(new int[]{x, 0}); }
            if (grid[x][d-1] == EMPTY) { grid[x][d-1] = OUTSIDE; queue.add(new int[]{x, d-1}); }
        }
        for (int z = 1; z < d-1; z++) {
            if (grid[0][z]   == EMPTY) { grid[0][z]   = OUTSIDE; queue.add(new int[]{0, z}); }
            if (grid[w-1][z] == EMPTY) { grid[w-1][z] = OUTSIDE; queue.add(new int[]{w-1, z}); }
        }
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int cx2 = cell[0], cz2 = cell[1];
            int[][] nb = {{cx2-1,cz2},{cx2+1,cz2},{cx2,cz2-1},{cx2,cz2+1}};
            for (int[] n : nb) {
                if (n[0] < 0 || n[1] < 0 || n[0] >= w || n[1] >= d) continue;
                if (grid[n[0]][n[1]] == EMPTY) {
                    grid[n[0]][n[1]] = OUTSIDE;
                    queue.add(n);
                }
            }
        }

        // 5. Perimeter: OUTSIDE cells adjacent to INTERIOR become wall cells
        for (int x = 1; x < w-1; x++) {
            for (int z = 1; z < d-1; z++) {
                if (grid[x][z] != OUTSIDE) continue;
                if (grid[x-1][z] == INTERIOR || grid[x+1][z] == INTERIOR ||
                    grid[x][z-1] == INTERIOR || grid[x][z+1] == INTERIOR)
                    grid[x][z] = WALL;
            }
        }
        // Re-mark remaining OUTSIDE cells as EMPTY for gate detection
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++)
                if (grid[x][z] == OUTSIDE) grid[x][z] = EMPTY;

        // 5b. Square diagonal WALL adjacencies. Perimeter detection naturally leaves the
        //     two wall runs around a 90-degree turn touching only diagonally. Fill the
        //     EMPTY shared square, not the INTERIOR shared square, so outside corners and
        //     inside notches read as square L-corners instead of clipped chamfers.
        squareWallCornerGaps(grid, w, d);

        // 6. Remove small disconnected wall loops — keep only the largest connected ring.
        //    Gap-fill sometimes creates separate interior zones that each get their own wall;
        //    those internal loops must be removed before placement.
        pruneSmallWallLoops(grid, w, d);

        // 7. Village-type materials
        int cx = minX + (maxX - minX) / 2;
        int cz = minZ + (maxZ - minZ) / 2;
        VillageBuildingMaterials.VillageType villageType = VillageBuildingMaterials.resolveVillageType(level, start);
        if (villageType == null) {
            villageType = VillageBuildingMaterials.typeForSample(level, cx, cz);
        }
        BlockState wallBlock   = wallMaterial(villageType);
        BlockState brickBlock  = brickMaterial(villageType);
        BlockState merlonBlock = merlonMaterial(villageType);
        BlockState stairBlock  = stairMaterial(villageType);

        // 7b. Detect gates as independent definitions (built after the wall, overriding it).
        //     No GATE cells are marked in the grid — the wall ring is solid and each gatehouse
        //     overrides whatever it covers. Road/path material is biome-aware (desert villages
        //     pave with smooth sandstone, not dirt path).
        boolean desert = villageType == VillageBuildingMaterials.VillageType.DESERT;
        BlockState pathBlock = desert ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
                                      : Blocks.DIRT_PATH.defaultBlockState();
        GateDetection gateDetection = detectGates(level, grid, originX, originZ, w, d, desert);
        List<GateDef> gates = gateDetection.gates();
        if (gateDetection.candidates() == 0) {
            WitcheryMod.LOGGER.info("[VillageWalls] No gate candidates for {}; postponing wall generation until roads are ready.",
                    start.getBoundingBox());
            return GenerationResult.skipped(0, 0);
        }

        // 8. Pre-sample wall base heights (water-aware). Walls follow terrain.
        int minBuild = MIN_BUILD_Y; // 1.21 minimum build height
        short[][] heights = new short[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != WALL) continue;
                int wx = originX + x, wz = originZ + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int h = wallBaseHeight(level, wx, wz); // water surface over water, else ground
                heights[x][z] = (short) Math.max(h, minBuild + 5);
            }
        }

        // 8b. Anti-dive: a wall cell may not sit more than MAX_DIP below its WALL neighbours,
        //     so a lone cave mouth / pit can't yank one column into the ground and disconnect
        //     the ring. Walls still follow general slopes; fillDown bridges the gap beneath.
        smoothWallDips(grid, heights, w, d);

        // 9a. Pre-clear vegetation/trees/mushrooms in every wall column.
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != WALL) continue;
                int wx = originX + x, wz = originZ + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int sy = heights[x][z];
                if (sy == 0) continue;
                clearTouchedVegetation(level, wx, sy, wz, WALL_HEIGHT + 5, TREE_CLEAR_WALL_TOUCH_RADIUS);
                for (int dy = 0; dy < WALL_HEIGHT + 5; dy++) {
                    BlockPos p = new BlockPos(wx, sy + dy, wz);
                    BlockState bs = level.getBlockState(p);
                    if (!bs.isAir() && isNaturalVegetation(bs))
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // 9. Place wall columns (gates are built afterwards, overriding the wall).
        int guardCounter = 0;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != WALL) continue;
                int wx = originX + x, wz = originZ + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int sy = heights[x][z];
                if (sy == 0) continue;

                // Extend the curtain down to the lowest along-wall neighbour so steps over
                // cliffs stay connected.
                int floorTo = sy;
                if (x > 0   && grid[x-1][z] == WALL && heights[x-1][z] > 0) floorTo = Math.min(floorTo, heights[x-1][z]);
                if (x < w-1 && grid[x+1][z] == WALL && heights[x+1][z] > 0) floorTo = Math.min(floorTo, heights[x+1][z]);
                if (z > 0   && grid[x][z-1] == WALL && heights[x][z-1] > 0) floorTo = Math.min(floorTo, heights[x][z-1]);
                if (z < d-1 && grid[x][z+1] == WALL && heights[x][z+1] > 0) floorTo = Math.min(floorTo, heights[x][z+1]);

                WallColumnResult column = placeWallColumn(level, wx, sy, wz, wallBlock, brickBlock,
                        merlonBlock, stairBlock, floorTo, grid, heights, x, z, w, d);
                if (spawnGuards && column.placed() && column.guardPos() != null && ++guardCounter >= GUARD_SPACING) {
                    spawnGuard(level, column.guardPos());
                    guardCounter = 0;
                }
            }
        }

        // 10. Build gatehouses — independent locked 7×3 models that override the wall.
        for (GateDef g : gates) buildGatehouse(level, g, brickBlock, merlonBlock, pathBlock, desert, spawnGuards);

        WitcheryMod.LOGGER.info("[VillageWalls] Generated walls for village at {},{} ({}x{}), {} gate(s).",
                cx, cz, w, d, gates.size());
        return new GenerationResult(true, gateDetection.candidates(), gates.size());
    }

    private static boolean isAbandonedVillage(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece poolPiece
                    && poolPiece.getElement().toString().contains("/zombie/")) {
                return true;
            }
        }
        return false;
    }

    // ── Loop pruning ───────────────────────────────────────────────────────────

    /**
     * Finds all connected components of WALL/GATE cells and discards every component
     * except the largest one. This eliminates internal loops created when gap-fill
     * produces isolated interior zones each wrapped in their own wall ring.
     */
    private static void pruneSmallWallLoops(byte[][] grid, int w, int d) {
        Set<Long> visited = new HashSet<>();
        List<List<Long>> components = new ArrayList<>();

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if ((grid[x][z] != WALL && grid[x][z] != GATE) || visited.contains(pack(x, z)))
                    continue;
                List<Long> comp = new ArrayList<>();
                ArrayDeque<long[]> q = new ArrayDeque<>();
                q.add(new long[]{x, z});
                while (!q.isEmpty()) {
                    long[] c = q.poll();
                    int cx = (int)c[0], cz = (int)c[1];
                    long key = pack(cx, cz);
                    if (!visited.add(key)) continue;
                    comp.add(key);
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = cx+dx, nz = cz+dz;
                        if (nx < 0 || nz < 0 || nx >= w || nz >= d) continue;
                        if ((grid[nx][nz] == WALL || grid[nx][nz] == GATE)
                                && !visited.contains(pack(nx, nz)))
                            q.add(new long[]{nx, nz});
                    }
                }
                components.add(comp);
            }
        }
        if (components.size() <= 1) return;

        Set<Long> keep = new HashSet<>(components.stream()
                .max(Comparator.comparingInt(List::size)).orElseThrow());

        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++)
                if ((grid[x][z] == WALL || grid[x][z] == GATE) && !keep.contains(pack(x, z)))
                    grid[x][z] = INTERIOR;
    }

    static void squareWallCornerGaps(byte[][] grid, int w, int d) {
        byte[][] snap = copyGrid(grid, w, d);
        for (int x = 1; x < w - 1; x++) {
            for (int z = 1; z < d - 1; z++) {
                if (snap[x][z] != WALL) continue;
                for (int dx : new int[]{-1, 1}) {
                    for (int dz : new int[]{-1, 1}) {
                        if (snap[x + dx][z + dz] != WALL) continue;

                        int ax = x + dx;
                        int az = z;
                        int bx = x;
                        int bz = z + dz;
                        if (snap[ax][az] == EMPTY && snap[bx][bz] == INTERIOR) {
                            grid[ax][az] = WALL;
                        } else if (snap[bx][bz] == EMPTY && snap[ax][az] == INTERIOR) {
                            grid[bx][bz] = WALL;
                        }
                    }
                }
            }
        }
    }

    private static long pack(int x, int z) { return (long)x << 20 | z; }

    private static boolean isNaturalVegetation(BlockState state) {
        return state.is(BlockTags.LEAVES)
            || state.is(BlockTags.LOGS)
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.SAPLINGS)
            || isMushroomBlock(state)
            || state.canBeReplaced();
    }

    private static boolean isTreeBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    private static boolean isMushroomBlock(BlockState state) {
        return state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM);
    }

    private static boolean isConnectedVegetationBlock(BlockState state) {
        return isTreeBlock(state) || isMushroomBlock(state);
    }

    /**
     * If a wall/gate/path corridor touches a tree or giant mushroom, remove the nearby connected
     * natural mass so generation does not leave floating canopies, half-cut trunks, or mushroom caps.
     */
    private static void clearTouchedVegetation(ServerLevel level, int centerX, int baseY, int centerZ,
                                               int heightAbove, int touchRadius) {
        int minY = Math.max(MIN_BUILD_Y, baseY - TREE_CLEAR_SCAN_Y_BELOW);
        int maxY = Math.min(MAX_BUILD_Y, baseY + heightAbove);
        for (int dx = -touchRadius; dx <= touchRadius; dx++) {
            for (int dz = -touchRadius; dz <= touchRadius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isConnectedVegetationBlock(level.getBlockState(pos))) {
                        clearConnectedVegetation(level, pos, centerX, centerZ, minY, maxY + TREE_CLEAR_SCAN_Y_ABOVE);
                    }
                }
            }
        }
    }

    private static void clearConnectedVegetation(ServerLevel level, BlockPos seed, int centerX, int centerZ,
                                                 int minY, int maxY) {
        maxY = Math.min(MAX_BUILD_Y, maxY);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(seed.immutable());
        int cleared = 0;
        while (!queue.isEmpty() && cleared < TREE_CLEAR_MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            if (!seen.add(pos)) continue;
            if (pos.getY() < minY || pos.getY() > maxY) continue;
            if (Math.abs(pos.getX() - centerX) > TREE_CLEAR_TREE_RADIUS
                    || Math.abs(pos.getZ() - centerZ) > TREE_CLEAR_TREE_RADIUS) continue;
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            if (!isConnectedVegetationBlock(level.getBlockState(pos))) continue;

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            cleared++;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        queue.add(pos.offset(dx, dy, dz).immutable());
                    }
                }
            }
        }
    }

    // ── Gate detection ─────────────────────────────────────────────────────────

    /** A gatehouse to build: world centre on the wall line, base height, inward + along-wall axes. */
    private record GateDef(int centerWX, int centerWZ, int refY,
                           int inDX, int inDZ, int axisDX, int axisDZ) {}

    /**
     * Finds where village streets exit the wall and returns a GateDef for the best few.
     * No grid cells are marked — gatehouses are built afterwards and override the wall.
     *
     * Phase 1: DIRT_PATH surface cells = road (no gravel/dirt → no farm/deposit false hits).
     * Phase 2: candidates = the END of a long straight street, traced to the wall cell it
     *          would exit through; keeps the approach dir and the street run length.
     * Phase 3: rank by length, keep up to MAX_GATES spaced >= MIN_SPACING apart, rejecting any
     *          gate over water or without land on both sides; refY = immediate gate path height.
     */
    private static GateDetection detectGates(ServerLevel level, byte[][] grid,
                                              int ox, int oz, int w, int d, boolean desert) {
        // Phase 1
        boolean[][] road = new boolean[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != INTERIOR) continue;
                int wx = ox + x, wz = oz + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int gy = groundHeight(level, wx, wz);
                if (isVillageRoad(level.getBlockState(new BlockPos(wx, gy - 1, wz)), desert))
                    road[x][z] = true;
            }
        }

        // Phase 2 — candidate = {wallX, wallZ, run, dirX, dirZ, tipX, tipZ}
        final int ROAD_GAP = 3;
        final int MIN_RUN  = 8;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        List<int[]> candidates = new ArrayList<>();
        for (int x = 1; x < w - 1; x++) {
            for (int z = 1; z < d - 1; z++) {
                if (!road[x][z]) continue;
                for (int[] dir : dirs) {
                    int run = roadRun(road, x, z, -dir[0], -dir[1], w, d, ROAD_GAP, 64);
                    if (run < MIN_RUN) continue;
                    if (roadWithin(road, x, z, dir[0], dir[1], w, d, ROAD_GAP)) continue;
                    for (int depth = 1; depth < Math.max(w, d); depth++) {
                        int nx = x + dir[0] * depth, nz = z + dir[1] * depth;
                        if (nx < 0 || nz < 0 || nx >= w || nz >= d) break;
                        if (grid[nx][nz] == INTERIOR) continue;
                        if (grid[nx][nz] == WALL
                                && isFlatGateFace(grid, nx, nz, dir[0], dir[1], w, d))
                            candidates.add(new int[]{nx, nz, run, dir[0], dir[1], x, z});
                        break;
                    }
                }
            }
        }

        // Phase 3 — select longest, spaced, on-land gates
        final int MAX_GATES   = 3;
        final int MIN_SPACING = 28;
        candidates.sort((a, b) -> Integer.compare(b[2], a[2]));
        List<GateDef> gates = new ArrayList<>();
        List<int[]> chosen = new ArrayList<>();
        for (int[] c : candidates) {
            int nx = c[0], nz = c[1], ddx = c[3], ddz = c[4], tipx = c[5], tipz = c[6];
            boolean farEnough = true;
            for (int[] ch : chosen) {
                long dx = nx - ch[0], dz = nz - ch[1];
                if (dx * dx + dz * dz < (long) MIN_SPACING * MIN_SPACING) { farEnough = false; break; }
            }
            if (!farEnough) continue;

            int cwx = ox + nx, cwz = oz + nz;
            // No gates on water: the gate cell and the cells just inside/outside must be land.
            if (isOverWater(level, cwx, cwz)
                    || isOverWater(level, cwx + ddx, cwz + ddz)   // outside
                    || isOverWater(level, cwx - ddx, cwz - ddz))  // inside
                continue;

            int refY = gateThresholdAvgY(level, cwx, cwz, ddx, ddz, desert);
            if (refY < 0) refY = wallBaseHeight(level, cwx, cwz);

            int axisDX = (ddx != 0) ? 0 : 1, axisDZ = (ddx != 0) ? 1 : 0; // along the wall
            gates.add(new GateDef(cwx, cwz, refY, -ddx, -ddz, axisDX, axisDZ));
            chosen.add(c);
            if (gates.size() >= MAX_GATES) break;
        }
        WitcheryMod.LOGGER.info("[VillageWalls] {} gate candidate(s) -> {} gate(s).",
                candidates.size(), gates.size());
        return new GateDetection(gates, candidates.size());
    }

    /**
     * Average only the immediate 3-wide path strips outside and inside the gate. Longer inward
     * road runs can pass houses or slopes and pull the threshold down, so the gate height is
     * anchored to the six terrain/path cells that actually meet the opening.
     */
    private static int gateThresholdAvgY(ServerLevel level, int cwx, int cwz,
                                         int ddx, int ddz, boolean desert) {
        // Keep the gate threshold local: three cells outside plus three cells inside.
        int axisDX = (ddx != 0) ? 0 : 1;
        int axisDZ = (ddx != 0) ? 1 : 0;
        int[][] sides = {
                {ddx, ddz},   // outside the wall
                {-ddx, -ddz}  // inside the wall
        };
        long sum = 0; int n = 0;
        for (int[] side : sides) {
            for (int o = -1; o <= 1; o++) {
                int ix = cwx + side[0] + axisDX * o;
                int iz = cwz + side[1] + axisDZ * o;
                if (level.getChunkSource().getChunkNow(ix >> 4, iz >> 4) == null) continue;
                int gy = groundHeight(level, ix, iz);
                if (level.getBlockState(new BlockPos(ix, gy, iz)).liquid()) continue;
                int surfaceY = gy - 1;
                if (!isGatePathTerrainColumn(level, ix, surfaceY, iz, surfaceY, desert)) continue;
                sum += gy; n++;
            }
        }
        return n > 0 ? Math.round((float) sum / (float) n) : -1;
    }

    /** True if the column at (wx,wz) has water above its solid floor. */
    private static boolean isOverWater(ServerLevel level, int wx, int wz) {
        if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) return false;
        int gy = groundHeight(level, wx, wz);
        return level.getBlockState(new BlockPos(wx, gy, wz)).liquid();
    }

    /**
     * Anti-dive: raise any wall cell that sits more than MAX_DIP below the highest of its WALL
     * neighbours, repeated until stable. Stops a lone pit/cave column from dropping out of the
     * ring; walls still follow general slopes and fillDown bridges the raised base downward.
     */
    private static void smoothWallDips(byte[][] grid, short[][] heights, int w, int d) {
        final int MAX_DIP = 3;
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 16) {
            changed = false;
            for (int x = 1; x < w - 1; x++) {
                for (int z = 1; z < d - 1; z++) {
                    if (grid[x][z] != WALL || heights[x][z] == 0) continue;
                    int hi = 0;
                    if (grid[x-1][z] == WALL) hi = Math.max(hi, heights[x-1][z]);
                    if (grid[x+1][z] == WALL) hi = Math.max(hi, heights[x+1][z]);
                    if (grid[x][z-1] == WALL) hi = Math.max(hi, heights[x][z-1]);
                    if (grid[x][z+1] == WALL) hi = Math.max(hi, heights[x][z+1]);
                    if (hi > 0 && heights[x][z] < hi - MAX_DIP) {
                        heights[x][z] = (short) (hi - MAX_DIP);
                        changed = true;
                    }
                }
            }
        }
    }

    /** True if any of the next {@code gap} cells from (x,z) in direction (dx,dz) is road. */
    private static boolean roadWithin(boolean[][] road, int x, int z,
                                       int dx, int dz, int w, int d, int gap) {
        for (int s = 1; s <= gap; s++) {
            int nx = x + dx * s, nz = z + dz * s;
            if (nx < 0 || nz < 0 || nx >= w || nz >= d) return false;
            if (road[nx][nz]) return true;
        }
        return false;
    }

    /**
     * Counts how many road cells lie along direction (dx,dz) from (x,z), bridging gaps of
     * up to {@code gap} non-road cells (so a dotted street still counts as continuous).
     * Stops after {@code max} steps or when a gap longer than {@code gap} is hit.
     */
    private static int roadRun(boolean[][] road, int x, int z, int dx, int dz,
                                int w, int d, int gap, int max) {
        int count = 0, miss = 0, cx = x, cz = z;
        for (int s = 0; s < max; s++) {
            cx += dx; cz += dz;
            if (cx < 0 || cz < 0 || cx >= w || cz >= d) break;
            if (road[cx][cz]) { count++; miss = 0; }
            else if (++miss > gap) break;
        }
        return count;
    }

    // ── Wall placement ─────────────────────────────────────────────────────────

    /**
     * A road may only create a gate where it reaches a straight wall face head-on.
     * This rejects corner and parallel-wall hits that would put the gatehouse sideways
     * against the nearby path instead of opening through the curtain.
     */
    private static boolean isFlatGateFace(byte[][] grid, int x, int z,
                                           int outDX, int outDZ, int w, int d) {
        int axisDX = (outDX != 0) ? 0 : 1;
        int axisDZ = (outDX != 0) ? 1 : 0;

        for (int o = -GATE_FLAT_FACE_HALF_WIDTH; o <= GATE_FLAT_FACE_HALF_WIDTH; o++) {
            int gx = x + axisDX * o;
            int gz = z + axisDZ * o;
            if (!insideGrid(gx, gz, w, d) || grid[gx][gz] != WALL) return false;

            int innerX = gx - outDX;
            int innerZ = gz - outDZ;
            int outerX = gx + outDX;
            int outerZ = gz + outDZ;
            if (!insideGrid(innerX, innerZ, w, d) || !insideGrid(outerX, outerZ, w, d))
                return false;
            if (grid[innerX][innerZ] != INTERIOR) return false;
            if (grid[outerX][outerZ] == INTERIOR || grid[outerX][outerZ] == WALL
                    || grid[outerX][outerZ] == GATE)
                return false;
        }
        return true;
    }

    private static boolean insideGrid(int x, int z, int w, int d) {
        return x >= 0 && z >= 0 && x < w && z < d;
    }

    /**
     * One wall column:
     *  - Stone brick base (2 courses)
     *  - Stone brick main shaft
     *  - Stone brick cap
     *  - 1-high stone brick wall merlon every 2 blocks (classic castle cadence)
     *  - Corner bonus: +2 height + 2-high merlon (tower effect)
     *
     * The curtain is extended down to {@code floorTo} (the lowest along-wall neighbour base)
     * so the wall stays connected across steps over cliffs; stone fills the foundation below.
     */
    private static WallColumnResult placeWallColumn(ServerLevel level, int wx, int sy, int wz,
                                                     BlockState wall, BlockState brick, BlockState merlon, BlockState stair,
                                                     int floorTo, byte[][] grid, short[][] heights,
                                                     int gx, int gz, int w, int d) {
        int top = sy + WALL_HEIGHT;
        if (!moveLivingEntitiesOutOfWallColumn(level, wx, Math.min(sy, floorTo), wz, top + 1,
                grid, gx, gz, w, d))
            return WallColumnResult.SKIPPED;

        // Base: 2 courses of stone brick (stone if submerged)
        placeWallMat(level, wx, sy,     wz, brick);
        placeWallMat(level, wx, sy + 1, wz, brick);
        // Main shaft (stone if submerged)
        for (int dy = 2; dy < WALL_HEIGHT - 1; dy++)
            placeWallMat(level, wx, sy + dy, wz, wall);
        // Cap course: stone brick
        setBlock(level, wx, top - 1, wz, brick);

        // Merlons: 1-high stone brick wall every 2 blocks (period=2, classic castle).
        int phase = Math.floorMod(wx + wz, 2);
        if (phase == 0) {
            setBlock(level, wx, top, wz, merlon);
        }

        BlockPos guardPos = null;
        if (canPlaceRampartDetails(grid, heights, gx, gz, sy, w, d)) {
            guardPos = placeRampartDetails(level, wx, top, wz, brick, stair,
                    grid, heights, gx, gz, sy, w, d);
        }

        // Extend the curtain down to the lowest neighbour so cliff steps stay connected
        // (stone wherever submerged, via placeWallMat).
        for (int y = sy - 1; y >= floorTo; y--) placeWallMat(level, wx, y, wz, wall);
        fillDown(level, wx, Math.min(sy, floorTo) - 1, wz, brick);
        return new WallColumnResult(true, guardPos);
    }

    /**
     * Witchery Walls' old curtain used block lips and stair caps on exposed top faces.
     * Keep the same visual language without widening the whole wall shaft: outer faces
     * get a stair-capped rampart lip, while the village side gets a narrow wall-walk ledge.
     */
    private static BlockPos placeRampartDetails(ServerLevel level, int wx, int top, int wz,
                                                 BlockState brick, BlockState stair,
                                                 byte[][] grid, short[][] heights,
                                                 int gx, int gz, int sy, int w, int d) {
        boolean northOutside = isGridCell(grid, gx, gz - 1, w, d, EMPTY);
        boolean southOutside = isGridCell(grid, gx, gz + 1, w, d, EMPTY);
        boolean eastOutside = isGridCell(grid, gx + 1, gz, w, d, EMPTY);
        boolean westOutside = isGridCell(grid, gx - 1, gz, w, d, EMPTY);

        BlockPos guardPos = null;
        if (isGridCell(grid, gx, gz - 1, w, d, INTERIOR)
                && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.NORTH, INTERIOR))
            guardPos = firstGuardPos(guardPos, placeWallWalk(level, wx, top, wz - 1, brick));
        if (isGridCell(grid, gx, gz + 1, w, d, INTERIOR)
                && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.SOUTH, INTERIOR))
            guardPos = firstGuardPos(guardPos, placeWallWalk(level, wx, top, wz + 1, brick));
        if (isGridCell(grid, gx + 1, gz, w, d, INTERIOR)
                && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.EAST, INTERIOR))
            guardPos = firstGuardPos(guardPos, placeWallWalk(level, wx + 1, top, wz, brick));
        if (isGridCell(grid, gx - 1, gz, w, d, INTERIOR)
                && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.WEST, INTERIOR))
            guardPos = firstGuardPos(guardPos, placeWallWalk(level, wx - 1, top, wz, brick));

        guardPos = placeCornerWallWalks(level, wx, top, wz, brick,
                grid, heights, gx, gz, sy, w, d, guardPos);

        if (northOutside && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.NORTH, EMPTY))
            placeRampartFace(level, wx, top, wz, brick, stair, Direction.NORTH);
        if (southOutside && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.SOUTH, EMPTY))
            placeRampartFace(level, wx, top, wz, brick, stair, Direction.SOUTH);
        if (eastOutside && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.EAST, EMPTY))
            placeRampartFace(level, wx, top, wz, brick, stair, Direction.EAST);
        if (westOutside && hasMatchingSideRampartNeighbor(grid, heights, gx, gz, sy, w, d, Direction.WEST, EMPTY))
            placeRampartFace(level, wx, top, wz, brick, stair, Direction.WEST);

        placeOuterCornerRamparts(level, wx, top, wz, brick, stair, grid, heights, gx, gz, sy, w, d);

        return guardPos;
    }

    private static BlockPos placeCornerWallWalks(ServerLevel level, int wx, int top, int wz,
                                                  BlockState brick, byte[][] grid, short[][] heights,
                                                  int gx, int gz, int sy, int w, int d,
                                                  BlockPos guardPos) {
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                if (!isGridCell(grid, gx + dx, gz + dz, w, d, INTERIOR)) continue;
                if (!isRampartCompatibleNeighbor(grid, heights, gx + dx, gz, sy, w, d)) continue;
                if (!isRampartCompatibleNeighbor(grid, heights, gx, gz + dz, sy, w, d)) continue;
                guardPos = firstGuardPos(guardPos, placeCornerWallWalk(level, wx + dx, top, wz + dz, brick));
            }
        }
        return guardPos;
    }

    private static void placeRampartFace(ServerLevel level, int wx, int top, int wz,
                                          BlockState brick, BlockState stair, Direction outward) {
        int dx = outward.getStepX();
        int dz = outward.getStepZ();
        setBlock(level, wx + dx, top - 1, wz + dz, brick);
        setBlock(level, wx + dx, top, wz + dz, stairFor(stair, outward));
    }

    private static void placeOuterCornerRamparts(ServerLevel level, int wx, int top, int wz,
                                                  BlockState brick, BlockState stair,
                                                  byte[][] grid, short[][] heights,
                                                  int gx, int gz, int sy, int w, int d) {
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                if (!isGridCell(grid, gx + dx, gz, w, d, EMPTY)) continue;
                if (!isGridCell(grid, gx, gz + dz, w, d, EMPTY)) continue;
                if (!isRampartCompatibleNeighbor(grid, heights, gx - dx, gz, sy, w, d)) continue;
                if (!isRampartCompatibleNeighbor(grid, heights, gx, gz - dz, sy, w, d)) continue;

                setBlock(level, wx + dx, top - 1, wz + dz, brick);
                setBlock(level, wx + dx, top, wz + dz, cornerStairFor(stair, dx, dz));
            }
        }
    }

    private static boolean isGridCell(byte[][] grid, int x, int z, int w, int d, byte value) {
        return insideGrid(x, z, w, d) && grid[x][z] == value;
    }

    private static boolean canPlaceRampartDetails(byte[][] grid, short[][] heights,
                                                   int gx, int gz, int sy, int w, int d) {
        return !hasSteepRampartStep(grid, heights, gx, gz, sy, w, d)
                && (isRampartCompatibleNeighbor(grid, heights, gx - 1, gz, sy, w, d)
                || isRampartCompatibleNeighbor(grid, heights, gx + 1, gz, sy, w, d)
                || isRampartCompatibleNeighbor(grid, heights, gx, gz - 1, sy, w, d)
                || isRampartCompatibleNeighbor(grid, heights, gx, gz + 1, sy, w, d));
    }

    private static boolean hasMatchingSideRampartNeighbor(byte[][] grid, short[][] heights,
                                                           int gx, int gz, int sy, int w, int d,
                                                           Direction side, byte sideValue) {
        if (side.getAxis() == Direction.Axis.Z) {
            return isMatchingSideRampartNeighbor(grid, heights, gx - 1, gz, sy, w, d, side, sideValue)
                    || isMatchingSideRampartNeighbor(grid, heights, gx + 1, gz, sy, w, d, side, sideValue);
        }
        return isMatchingSideRampartNeighbor(grid, heights, gx, gz - 1, sy, w, d, side, sideValue)
                || isMatchingSideRampartNeighbor(grid, heights, gx, gz + 1, sy, w, d, side, sideValue);
    }

    private static boolean isMatchingSideRampartNeighbor(byte[][] grid, short[][] heights,
                                                          int gx, int gz, int sy, int w, int d,
                                                          Direction side, byte sideValue) {
        return isRampartCompatibleNeighbor(grid, heights, gx, gz, sy, w, d)
                && isGridCell(grid, gx + side.getStepX(), gz + side.getStepZ(), w, d, sideValue);
    }

    private static boolean isRampartCompatibleNeighbor(byte[][] grid, short[][] heights,
                                                        int gx, int gz, int sy, int w, int d) {
        if (!insideGrid(gx, gz, w, d) || grid[gx][gz] != WALL || heights[gx][gz] == 0) return false;
        return Math.abs(heights[gx][gz] - sy) <= 1
                && !hasSteepRampartStep(grid, heights, gx, gz, heights[gx][gz], w, d);
    }

    private static BlockPos placeWallWalk(ServerLevel level, int x, int top, int z, BlockState brick) {
        BlockPos floor = new BlockPos(x, top - 1, z);
        if (level.getChunkSource().getChunkNow(floor.getX() >> 4, floor.getZ() >> 4) == null) return null;
        setBlock(level, floor.getX(), floor.getY(), floor.getZ(), brick);
        BlockPos stand = floor.above();
        if (!level.getBlockState(floor).isSolid() || !isSpawnClear(level, stand)) return null;
        return stand;
    }

    private static BlockPos placeCornerWallWalk(ServerLevel level, int x, int top, int z, BlockState brick) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return null;
        forceSet(level, x, top - 1, z, brick);
        forceSet(level, x, top, z, Blocks.AIR.defaultBlockState());
        forceSet(level, x, top + 1, z, Blocks.AIR.defaultBlockState());
        BlockPos floor = new BlockPos(x, top - 1, z);
        BlockPos stand = floor.above();
        if (!level.getBlockState(floor).isSolid() || !isSpawnClear(level, stand)) return null;
        return stand;
    }

    private static BlockPos firstGuardPos(BlockPos current, BlockPos candidate) {
        return current != null ? current : candidate;
    }

    private static boolean isSpawnClear(ServerLevel level, BlockPos pos) {
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        return (feet.isAir() || feet.canBeReplaced()) && (head.isAir() || head.canBeReplaced());
    }

    private static boolean hasSteepRampartStep(byte[][] grid, short[][] heights,
                                                int gx, int gz, int sy, int w, int d) {
        return hasSteepWallNeighbor(grid, heights, gx - 1, gz, sy, w, d)
                || hasSteepWallNeighbor(grid, heights, gx + 1, gz, sy, w, d)
                || hasSteepWallNeighbor(grid, heights, gx, gz - 1, sy, w, d)
                || hasSteepWallNeighbor(grid, heights, gx, gz + 1, sy, w, d);
    }

    private static boolean hasSteepWallNeighbor(byte[][] grid, short[][] heights,
                                                 int gx, int gz, int sy, int w, int d) {
        if (!insideGrid(gx, gz, w, d) || grid[gx][gz] != WALL || heights[gx][gz] == 0) return false;
        return Math.abs(heights[gx][gz] - sy) > 1;
    }

    private static BlockState stairFor(BlockState base, Direction facing) {
        return base.setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
    }

    static BlockState cornerStairFor(BlockState base, int dx, int dz) {
        // Match vanilla outer-stair shaping: face the diagonal cap toward the
        // adjacent north/south rampart stair, then choose left/right from that
        // neighbor's facing.
        Direction facing = dx < 0 ? Direction.EAST : Direction.WEST;
        Direction neighborFacing = dz < 0 ? Direction.NORTH : Direction.SOUTH;
        StairsShape shape = neighborFacing == facing.getCounterClockWise()
                ? StairsShape.OUTER_LEFT
                : StairsShape.OUTER_RIGHT;
        return base.setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, shape);
    }

    private static boolean moveLivingEntitiesOutOfWallColumn(ServerLevel level, int x, int minY, int z, int maxY,
                                                              byte[][] grid, int gx, int gz, int w, int d) {
        AABB box = new AABB(x, minY, z, x + 1.0, maxY + 1.0, z + 1.0);
        List<LivingEntity> blockers = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        if (blockers.isEmpty()) return true;

        for (LivingEntity blocker : blockers) {
            BlockPos safe = findWallBlockerRelocation(level, x, z, grid, gx, gz, w, d, blocker);
            if (safe == null) {
                WitcheryMod.LOGGER.info("[VillageWalls] Could not safely move {} out of wall column {},{}; skipping column.",
                        blocker.getType(), x, z);
                return false;
            }
            blocker.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
            WitcheryMod.LOGGER.info("[VillageWalls] Moved {} from wall column {},{} to {},{},{} before placing wall.",
                    blocker.getType(), x, z,
                    safe.getX(), safe.getY(), safe.getZ());
        }

        return level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty();
    }

    private static BlockPos findWallBlockerRelocation(ServerLevel level, int wx, int wz,
                                                       byte[][] grid, int gx, int gz, int w, int d,
                                                       LivingEntity entity) {
        for (int radius = 1; radius <= WALL_ENTITY_RELOCATION_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int candidateGX = gx + dx;
                    int candidateGZ = gz + dz;
                    if (!insideGrid(candidateGX, candidateGZ, w, d)) continue;
                    byte cell = grid[candidateGX][candidateGZ];
                    if (cell == WALL || cell == GATE) continue;

                    int candidateX = wx + dx;
                    int candidateZ = wz + dz;
                    BlockPos candidate = safeRelocationPos(level, candidateX, candidateZ, entity);
                    if (candidate != null) return candidate;
                }
            }
        }
        return null;
    }

    private static BlockPos safeRelocationPos(ServerLevel level, int x, int z, LivingEntity entity) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return null;
        int y = groundHeight(level, x, z);
        BlockPos stand = new BlockPos(x, y, z);
        if (!level.getBlockState(stand.below()).isSolid()) return null;
        if (!isRelocationSpace(level, stand) || !isRelocationSpace(level, stand.above())) return null;

        double halfWidth = Math.max(0.3D, entity.getBbWidth() * 0.5D);
        double height = Math.max(1.0D, entity.getBbHeight());
        double cx = x + 0.5D;
        double cz = z + 0.5D;
        AABB targetBox = new AABB(cx - halfWidth, y, cz - halfWidth,
                cx + halfWidth, y + height, cz + halfWidth);
        if (!level.getEntitiesOfClass(LivingEntity.class, targetBox,
                other -> other.isAlive() && other != entity).isEmpty())
            return null;
        return level.noCollision(entity, targetBox) ? stand : null;
    }

    private static boolean isRelocationSpace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.liquid() && (state.isAir() || state.canBeReplaced());
    }

    /**
     * Builds one gatehouse as an independent locked 7×3 model, OVERRIDING whatever wall or
     * terrain it covers. It is built straight along the wall axis from the centre + reference
     * height in the GateDef, so it never bends with a corner and its side towers are always
     * intact regardless of nearby corners.
     *
     * Layout along the wall axis (offset o): |o|=2,3 → solid stone-brick PILLAR (2 taller than
     * the wall); |o|<=1 → OPENING (3-deep cleared passage, stone-brick arch, fence-less, path
     * floor). Matching fillDown bridges the structure down to the terrain/sea floor, and the path
     * is laid out and in (meeting the existing road) for the central 3 lanes.
     */
    private static void buildGatehouse(ServerLevel level, GateDef g, BlockState brick, BlockState gateCap,
                                        BlockState pathBlock, boolean desert, boolean spawnGuards) {
        BlockState pathSupport = pathSupportMaterial(level, g.centerWX(), g.centerWZ(), desert);
        int refY = g.refY();

        clearGatehouseFootprint(level, g);
        clearGatehouseInsideLedgeBlocks(level, g, brick);

        // Depth -1..+1 centres the 3-deep gatehouse on the wall line, so the wall meets the
        // gate at the middle of its sides (one block of the gate sits outside the wall).
        for (int o = -GATEHOUSE_HALF_WIDTH; o <= GATEHOUSE_HALF_WIDTH; o++) {
            int bx = g.centerWX() + g.axisDX() * o, bz = g.centerWZ() + g.axisDZ() * o;
            boolean opening = Math.abs(o) <= 1;
            for (int depth = -1; depth <= 1; depth++) {
                int dwx = bx + g.inDX() * depth, dwz = bz + g.inDZ() * depth;
                clearTouchedVegetation(level, dwx, refY - 1, dwz, WALL_HEIGHT + GATEHOUSE_CLEAR_ABOVE_WALL, TREE_CLEAR_WALL_TOUCH_RADIUS);
                if (opening) {
                    for (int dy = 0; dy < 5; dy++) forceSet(level, dwx, refY + dy, dwz, Blocks.AIR.defaultBlockState());
                    for (int dy = 5; dy < WALL_HEIGHT; dy++) forceSet(level, dwx, refY + dy, dwz, brick); // arch
                    forceSet(level, dwx, refY - 1, dwz, pathBlock);                                       // floor
                    fillDown(level, dwx, refY - 2, dwz, pathSupport);
                } else {
                    for (int dy = 0; dy <= WALL_HEIGHT + 1; dy++) forceSet(level, dwx, refY + dy, dwz, brick);
                    forceSet(level, dwx, refY + WALL_HEIGHT + 2, dwz, gateCap);
                    fillDown(level, dwx, refY - 1, dwz, brick);
                }
            }
        }

        // Path through the 3 opening lanes — GRADED so a hill outside can't block the gate.
        // Starts from the gate's outer/inner edge cell (depth ±1, floor refY-1) and ramps to
        // the terrain at most 1 block per cell, cutting headroom so there is never a dirt face
        // across the opening. Inward stops when it meets the existing village road.
        int oDX = -g.inDX(), oDZ = -g.inDZ();
        for (int o = -1; o <= 1; o++) {
            int bx = g.centerWX() + g.axisDX() * o, bz = g.centerWZ() + g.axisDZ() * o;
            gradePath(level, bx + oDX, bz + oDZ, oDX, oDZ, refY - 1, GATE_PATH_OUTWARD_MAX, false, false, pathBlock, pathSupport, desert);
            gradePath(level, bx + g.inDX(), bz + g.inDZ(), g.inDX(), g.inDZ(), refY - 1, GATE_PATH_INWARD_MAX, true, true, pathBlock, pathSupport, desert);
        }

        // Place the high gatehouse walk last so the terrain-only inward path grader sees the
        // terrain below the ledge, not the ledge itself as a blocking building column.
        buildGatehouseGuardRampart(level, g, brick, gateCap, spawnGuards);
    }

    /** Clears old wall and rampart blocks from the locked 7x3 gatehouse footprint. */
    private static void clearGatehouseFootprint(ServerLevel level, GateDef g) {
        for (int o = -GATEHOUSE_HALF_WIDTH; o <= GATEHOUSE_HALF_WIDTH; o++) {
            int bx = g.centerWX() + g.axisDX() * o;
            int bz = g.centerWZ() + g.axisDZ() * o;
            for (int depth = -1; depth <= 1; depth++) {
                int x = bx + g.inDX() * depth;
                int z = bz + g.inDZ() * depth;
                for (int dy = 0; dy <= WALL_HEIGHT + GATEHOUSE_CLEAR_ABOVE_WALL; dy++) {
                    forceSet(level, x, g.refY() + dy, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** Clears the previous version of the inside ledge before the inward path grades terrain. */
    private static void clearGatehouseInsideLedgeBlocks(ServerLevel level, GateDef g, BlockState brick) {
        int minY = Math.max(MIN_BUILD_Y, g.refY() + WALL_HEIGHT - 3);
        int maxY = Math.min(MAX_BUILD_Y, g.refY() + WALL_HEIGHT + GATEHOUSE_CLEAR_ABOVE_WALL);
        int insideDX = g.inDX();
        int insideDZ = g.inDZ();
        for (int o = -GATEHOUSE_HALF_WIDTH - 1; o <= GATEHOUSE_HALF_WIDTH + 1; o++) {
            int x = g.centerWX() + g.axisDX() * o + insideDX * 2;
            int z = g.centerWZ() + g.axisDZ() * o + insideDZ * 2;
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (isWallLedgeMaterial(level.getBlockState(pos), brick)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean isWallLedgeMaterial(BlockState state, BlockState brick) {
        return state.is(brick.getBlock())
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.ORANGE_TERRACOTTA);
    }

    private static void buildGatehouseGuardRampart(ServerLevel level, GateDef g,
                                                    BlockState brick, BlockState rail,
                                                    boolean spawnGuards) {
        int floorY = g.refY() + WALL_HEIGHT - 1;
        int standY = floorY + 1;
        for (int o = -1; o <= 1; o++) {
            int walkX = g.centerWX() + g.axisDX() * o;
            int walkZ = g.centerWZ() + g.axisDZ() * o;
            forceSet(level, walkX, floorY, walkZ, brick);

            int outsideX = walkX - g.inDX();
            int outsideZ = walkZ - g.inDZ();
            forceSet(level, outsideX, standY, outsideZ, rail);
        }

        int insideDX = g.inDX();
        int insideDZ = g.inDZ();
        for (int o = -GATEHOUSE_HALF_WIDTH - 1; o <= GATEHOUSE_HALF_WIDTH + 1; o++) {
            int walkX = g.centerWX() + g.axisDX() * o + insideDX * 2;
            int walkZ = g.centerWZ() + g.axisDZ() * o + insideDZ * 2;
            forceSet(level, walkX, floorY, walkZ, brick);
            forceSet(level, walkX, standY, walkZ, Blocks.AIR.defaultBlockState());
            forceSet(level, walkX, standY + 1, walkZ, Blocks.AIR.defaultBlockState());
        }

        if (spawnGuards) {
            placeGatehouseLanterns(level, g, rail);
            BlockPos guardPos = new BlockPos(g.centerWX(), standY, g.centerWZ());
            if (isSpawnClear(level, guardPos)) spawnGuard(level, guardPos);
        }
    }

    private static void placeGatehouseLanterns(ServerLevel level, GateDef g, BlockState bracket) {
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        int outsideDX = -g.inDX();
        int outsideDZ = -g.inDZ();
        Direction towardGatehouse = horizontalDirection(g.inDX(), g.inDZ());
        int bracketY = g.refY() + 4;
        int lanternY = bracketY - 1;
        for (int o : new int[]{-2, 2}) {
            int x = g.centerWX() + g.axisDX() * o + outsideDX * 2;
            int z = g.centerWZ() + g.axisDZ() * o + outsideDZ * 2;
            forceSet(level, x, bracketY, z, bracketConnectedToward(bracket, towardGatehouse));
            forceSet(level, x, lanternY, z, lantern);
        }
    }

    private static Direction horizontalDirection(int dx, int dz) {
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    private static BlockState bracketConnectedToward(BlockState state, Direction direction) {
        if (state.getBlock() instanceof WallBlock) {
            state = state.setValue(WallBlock.UP, true);
            return switch (direction) {
                case NORTH -> state.setValue(BlockStateProperties.NORTH_WALL, WallSide.LOW);
                case EAST -> state.setValue(BlockStateProperties.EAST_WALL, WallSide.LOW);
                case SOUTH -> state.setValue(BlockStateProperties.SOUTH_WALL, WallSide.LOW);
                case WEST -> state.setValue(BlockStateProperties.WEST_WALL, WallSide.LOW);
                default -> state;
            };
        }
        if (state.getBlock() instanceof FenceBlock) {
            return switch (direction) {
                case NORTH -> state.hasProperty(BlockStateProperties.NORTH)
                        ? state.setValue(BlockStateProperties.NORTH, true) : state;
                case EAST -> state.hasProperty(BlockStateProperties.EAST)
                        ? state.setValue(BlockStateProperties.EAST, true) : state;
                case SOUTH -> state.hasProperty(BlockStateProperties.SOUTH)
                        ? state.setValue(BlockStateProperties.SOUTH, true) : state;
                case WEST -> state.hasProperty(BlockStateProperties.WEST)
                        ? state.setValue(BlockStateProperties.WEST, true) : state;
                default -> state;
            };
        }
        return state;
    }

    /**
     * Lays a graded path from a start cell in direction (dx,dz): the floor ramps toward the
     * natural ground at most 1 block per cell, headroom is cleared above (cutting through
     * rising terrain so a hill can't wall off the gate), and any gap below is filled. If
     * {@code stopAtRoad}, it ends once it reaches an existing village road. If
     * {@code terrainOnly}, it stops before touching village/building blocks.
     */
    private static void gradePath(ServerLevel level, int startWX, int startWZ, int dx, int dz,
                                   int startFloorY, int maxLen, boolean stopAtRoad, boolean terrainOnly,
                                   BlockState pathBlock, BlockState supportBlock, boolean desert) {
        int floorY = startFloorY;
        for (int t = 1; t <= maxLen; t++) {
            int ix = startWX + dx * t, iz = startWZ + dz * t;
            if (level.getChunkSource().getChunkNow(ix >> 4, iz >> 4) == null) break;
            int natural = groundHeight(level, ix, iz) - 1; // natural surface block Y
            if (stopAtRoad && isVillageRoad(level.getBlockState(new BlockPos(ix, natural, iz)), desert)) break;
            int nextFloorY = floorY;
            if (natural > nextFloorY) nextFloorY++;
            else if (natural < nextFloorY) nextFloorY--;
            if (terrainOnly && !isGatePathTerrainColumn(level, ix, nextFloorY, iz, natural, desert)) break;
            floorY = nextFloorY;
            // Cut down any tree in the gate path on both lanes (clears connected logs/leaves only,
            // so houses are never demolished — only regular trees in the way are removed).
            clearTouchedVegetation(level, ix, floorY, iz, TREE_CLEAR_SCAN_Y_ABOVE, TREE_CLEAR_PATH_TOUCH_RADIUS);
            forceSet(level, ix, floorY, iz, pathBlock);                         // path surface
            for (int dy = 1; dy <= 4; dy++) {                                   // cut headroom
                BlockPos p = new BlockPos(ix, floorY + dy, iz);
                BlockState bs = level.getBlockState(p);
                if (!bs.isAir() && !bs.liquid()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            fillDown(level, ix, floorY - 1, iz, supportBlock); // support if floating
        }
    }

    private static boolean isGatePathTerrainColumn(ServerLevel level, int x, int floorY, int z,
                                                    int surfaceY, boolean desert) {
        int minY = Math.min(floorY, surfaceY);
        int maxY = Math.max(floorY, surfaceY) + 4;
        for (int y = minY; y <= maxY; y++) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (isGatePathTerrainBlock(state, desert)) continue;
            // A bare tree trunk is passable: the path grader cuts it down (clearTouchedVegetation).
            // House materials (planks/cobblestone/glass/etc.) are not terrain and are not logs, so
            // they still fail here and the grader stops — the building is left untouched.
            if (state.is(BlockTags.LOGS)) continue;
            return false;
        }
        return true;
    }

    private static boolean isGatePathTerrainBlock(BlockState state, boolean desert) {
        return state.isAir()
                || state.liquid()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || isMushroomBlock(state)
                || isVillageRoad(state, desert)
                || isNaturalGroundBlock(state);
    }

    private static boolean isNaturalGroundBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.DEEPSLATE);
    }

    /** Village street surface: DIRT_PATH everywhere; sandstone family in desert villages. */
    private static boolean isVillageRoad(BlockState bs, boolean desert) {
        if (bs.is(Blocks.DIRT_PATH)) return true;
        return desert && (bs.is(Blocks.SMOOTH_SANDSTONE) || bs.is(Blocks.SANDSTONE)
                       || bs.is(Blocks.CUT_SANDSTONE));
    }

    private static BlockState pathSupportMaterial(ServerLevel level, int x, int z, boolean desert) {
        if (!desert) return Blocks.DIRT.defaultBlockState();
        return level.getBiome(new BlockPos(x, 64, z)).is(BiomeTags.IS_BADLANDS)
                ? Blocks.RED_SAND.defaultBlockState()
                : Blocks.SAND.defaultBlockState();
    }

    /** Unconditional block set (overrides existing), chunk-load guarded. */
    private static void forceSet(ServerLevel level, int x, int y, int z, BlockState state) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return;
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    // ── Guard spawning ─────────────────────────────────────────────────────────

    private static void spawnGuard(ServerLevel level, BlockPos pos) {
        VillageGuardEntity guard = WitcheryEntities.VILLAGE_GUARD.get()
                .create(level, EntitySpawnReason.STRUCTURE);
        if (guard == null) return;
        guard.setPersistenceRequired();
        guard.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        guard.finalizeSpawn(level,
                new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(guard);
    }

    // ── Village-type materials ────────────────────────────────────────────────

    private static BlockState wallMaterial(VillageBuildingMaterials.VillageType villageType) {
        // Stone-brick family only — no cobblestone.
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.SANDSTONE.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.TAIGA)
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static BlockState brickMaterial(VillageBuildingMaterials.VillageType villageType) {
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.CHISELED_SANDSTONE.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** Battlement / merlon material — biome-adaptive wall-block variant. */
    private static BlockState merlonMaterial(VillageBuildingMaterials.VillageType villageType) {
        // Stone-brick family only — no cobblestone wall.
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.SANDSTONE_WALL.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.RED_SANDSTONE_WALL.defaultBlockState();
        return Blocks.STONE_BRICK_WALL.defaultBlockState();
    }

    /** Stair caps used for the Witchery Walls-style rampart lips. */
    private static BlockState stairMaterial(VillageBuildingMaterials.VillageType villageType) {
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.SANDSTONE_STAIRS.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.RED_SANDSTONE_STAIRS.defaultBlockState();
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState();
    }

    // ── Block helpers ──────────────────────────────────────────────────────────

    private static void setBlock(ServerLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return;
        BlockState existing = level.getBlockState(pos);
        // Replace air, liquids, replaceable decoration, and natural vegetation.
        // Logs and leaves are solid+non-replaceable in 1.21 but must be cleared for walls.
        if (!existing.isAir() && !existing.liquid() && !existing.canBeReplaced()
                && !isConnectedVegetationBlock(existing)) return;
        level.setBlock(pos, state, 3);
    }

    // Foundation / underwater fill: deep enough to bridge a wall sitting at the water
    // surface all the way down to the sea floor.
    private static void fillDown(ServerLevel level, int x, int startY, int z, BlockState block) {
        for (int y = startY; y >= startY - 40; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) break;
            if (level.getBlockState(pos).isSolid()) break;
            level.setBlock(pos, block, 3);
        }
    }

    /**
     * Wall base Y. On land = {@link #groundHeight}; over water = the water-surface level so the
     * wall sits at sea level (fillDown bridges the water column to the sea floor with stone).
     * Uses WORLD_SURFACE (top non-air block, which over water is the water surface) for a
     * robust water-surface read regardless of how groundHeight scanned the column.
     */
    private static int wallBaseHeight(ServerLevel level, int wx, int wz) {
        int ground = groundHeight(level, wx, wz);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
        if (surface > ground && level.getBlockState(new BlockPos(wx, surface - 1, wz)).liquid())
            return surface; // standing water above the ground → build at the surface
        return ground;
    }

    /**
     * Places wall material, but substitutes STONE wherever the target cell is liquid, so the
     * wall never shows its facing blocks under water (the submerged part reads as foundation).
     * Leaves solid village/terrain blocks untouched (same skip rule as {@link #setBlock}).
     */
    private static void placeWallMat(ServerLevel level, int x, int y, int z, BlockState wall) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return;
        BlockState ex = level.getBlockState(new BlockPos(x, y, z));
        if (ex.liquid()) { level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3); return; }
        if (!ex.isAir() && !ex.canBeReplaced() && !isConnectedVegetationBlock(ex)) return;
        level.setBlock(new BlockPos(x, y, z), wall, 3);
    }

    private static byte[][] copyGrid(byte[][] src, int w, int d) {
        byte[][] copy = new byte[w][d];
        for (int x = 0; x < w; x++) System.arraycopy(src[x], 0, copy[x], 0, d);
        return copy;
    }

    /**
     * Returns the tree-corrected ground surface Y for a column: scans down from the
     * MOTION_BLOCKING heightmap past air, liquids, leaves, logs, and replaceable
     * vegetation to the first solid ground block, returning the Y just above it.
     *
     * MOTION_BLOCKING alone counts leaves and logs, so on a tree-covered cell it
     * returns the treetop — building the wall there would float it on the canopy.
     * This helper finds the real ground beneath the tree instead.
     */
    private static int groundHeight(ServerLevel level, int wx, int wz) {
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
        for (int i = 0; i < 48; i++) {
            BlockState bs = level.getBlockState(new BlockPos(wx, h - 1, wz));
            if (bs.isAir() || bs.liquid() || isNaturalVegetation(bs)) { h--; continue; }
            break;
        }
        return h;
    }

}
