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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates adaptive fortress walls around a vanilla village.
 * Visible block template: Witchery 1.7.10 WorldHandlerVillageDistrict, adapted to 1.21.11.
 * The modern contour, biome palette, terrain, road, and chunk-scheduling systems remain.
 *
 * Algorithm:
 *  1. Collect all village piece bounding boxes and find extents.
 *  2. Build a 2D occupancy grid (1 cell = 1 world block).
 *  3. Fill narrow gaps between structures (range-4 pass).
 *  4. Identify perimeter cells (empty, adjacent to interior).
 *  5. Detect gate positions by scanning for DIRT_PATH blocks.
 *  6. Stamp the source wall's 3-wide curtain and recessed 5-wide crown along the perimeter.
 *  7. Carve the source wall's integrated 3-wide gate arches at road exits.
 *  8. Spawn guards on the recessed patrol deck every ~150 usable wall-walk blocks.
 */
public final class VillageWallGenerator {
    public record GenerationResult(boolean generated, int gateCandidates, int gates) {
        public static GenerationResult skipped(int gateCandidates, int gates) {
            return new GenerationResult(false, gateCandidates, gates);
        }
    }

    private record GateDetection(List<GateDef> gates, int candidates) {}

    enum SourceWallPart {
        MASONRY,
        ARCH_WALL,
        STAIR
    }

    record SourceWallBlock(BlockPos offset, SourceWallPart part, Direction facing, Half half, int priority) {}

    private record PlannedWallBlock(BlockState state, boolean force, int priority) {}
    private record HorizontalPos(int x, int z) {}

    private static final class WallBuildPlan {
        private final Map<BlockPos, PlannedWallBlock> blocks = new LinkedHashMap<>();
        private final Map<HorizontalPos, Integer> foundationStarts = new LinkedHashMap<>();
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        private void add(int x, int y, int z, BlockState state, boolean force, int priority) {
            BlockPos pos = new BlockPos(x, y, z);
            PlannedWallBlock next = new PlannedWallBlock(state, force, priority);
            blocks.merge(pos, next, (current, candidate) ->
                    candidate.priority() > current.priority() ? candidate : current);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        private void addFoundationColumn(int x, int startY, int z) {
            foundationStarts.merge(new HorizontalPos(x, z), startY, Math::min);
        }

        private boolean intersects(AABB box) {
            int minX = (int) Math.floor(box.minX);
            int minY = (int) Math.floor(box.minY);
            int minZ = (int) Math.floor(box.minZ);
            int maxX = (int) Math.ceil(box.maxX) - 1;
            int maxY = (int) Math.ceil(box.maxY) - 1;
            int maxZ = (int) Math.ceil(box.maxZ) - 1;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (blocks.containsKey(new BlockPos(x, y, z))) return true;
                    }
                }
            }
            return false;
        }

        private void removeIntersecting(AABB box) {
            blocks.keySet().removeIf(pos -> blockIntersects(pos, box));
        }

        private AABB bounds() {
            return blocks.isEmpty() ? null : new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        }

        private static boolean blockIntersects(BlockPos pos, AABB box) {
            return pos.getX() < box.maxX && pos.getX() + 1.0D > box.minX
                    && pos.getY() < box.maxY && pos.getY() + 1.0D > box.minY
                    && pos.getZ() < box.maxZ && pos.getZ() + 1.0D > box.minZ;
        }
    }

    private static final int WALL_HEIGHT   = 8;   // source curtain courses above surface
    private static final int GATE_CLEAR_ABOVE_WALL = 4;
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
    private static final int GATE_FLAT_APRON_DEPTH = 2;
    private static final int SOURCE_GATE_HALF_WIDTH = 2;
    private static final int SOURCE_GATE_DEPTH = 1;
    private static final int SOURCE_GATE_OPENING_HALF_WIDTH = 1;
    private static final int SOURCE_GATE_ARCH_DROP = 3;
    private static final int PLAN_PRIORITY_STAIR = 10;
    private static final int PLAN_PRIORITY_RAIL = 20;
    private static final int PLAN_PRIORITY_CORNER = 30;
    private static final int PLAN_PRIORITY_BODY = 40;
    private static final int TREE_CLEAR_MAX_BLOCKS = 384;
    private static final int TREE_CLEAR_SCAN_Y_ABOVE = 24;
    private static final int TREE_CLEAR_SCAN_Y_BELOW = 2;
    private static final int TREE_CLEAR_WALL_TOUCH_RADIUS = 2;
    private static final int TREE_CLEAR_PATH_TOUCH_RADIUS = 1;
    private static final int TREE_CLEAR_TREE_RADIUS = 7;
    private static final int WALL_ENTITY_RELOCATION_RADIUS = 12;
    private static final int WALL_ENTITY_RELOCATION_VERTICAL_LIMIT = 4;
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

    /** Default chance (percent) that a detected village receives walls. */
    public static final int DEFAULT_WALL_SPAWN_PERCENT = 40;

    /**
     * Deterministic per-village decision: true if this village should receive walls.
     * Seeded from the world seed and the village bounding-box min corner so the same
     * village always decides the same way across reloads and regardless of chunk-load
     * order (the same start can be evaluated many times). The coordinate multipliers are
     * vanilla-style so symmetric/swapped {@code (minX, minZ)} pairs don't collide.
     */
    public static boolean shouldGenerateWalls(long worldSeed, int minX, int minZ, int spawnPercent) {
        if (spawnPercent >= 100) return true;
        if (spawnPercent <= 0) return false;
        long seed = worldSeed
                ^ ((long) minX * 341873128712L)
                ^ ((long) minZ * 132897987541L);
        return net.minecraft.util.RandomSource.create(seed).nextInt(100) < spawnPercent;
    }

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
        BlockState wallBlock  = wallMaterial(villageType);
        BlockState archBlock  = archMaterial(villageType);
        BlockState stairBlock = stairMaterial(villageType);

        // 7b. Detect gates independently. The grid remains a solid ring; integrated source
        //     arches are carved after the curtain. Desert village roads remain sandstone.
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

        // 9. Build and apply the deduplicated source-style curtain.
        WallBuildPlan wallPlan = new WallBuildPlan();
        List<BlockPos> guardCandidates = new ArrayList<>();
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

                addSourceWallStamp(wallPlan, wx, sy, wz, floorTo, wallBlock, stairBlock,
                        grid, x, z, w, d);
                guardCandidates.add(new BlockPos(wx, sy + WALL_HEIGHT, wz));
            }
        }

        relocatePlanBlockers(level, wallPlan, "wall");
        applyWallPlan(level, wallPlan, wallBlock);

        // 10. Carve road-aligned source arches through the completed curtain.
        for (GateDef g : gates) buildSourceGate(level, g, wallBlock, archBlock, stairBlock,
                pathBlock, desert, spawnGuards);

        if (spawnGuards) {
            int guardCounter = 0;
            for (BlockPos candidate : guardCandidates) {
                if (isInsideGateCrown(candidate, gates)) continue;
                if (!level.getBlockState(candidate.below()).isSolid() || !isSpawnClear(level, candidate)) continue;
                if (++guardCounter >= GUARD_SPACING) {
                    spawnGuard(level, candidate);
                    guardCounter = 0;
                }
            }
        }
        rescueCollidingLivingEntities(level, wallPlan,
                minX - PADDING, minZ - PADDING, maxX + PADDING, maxZ + PADDING);

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

    /** An integrated gate: world center, threshold height, inward axis, and along-wall axis. */
    private record GateDef(int centerWX, int centerWZ, int refY,
                           int inDX, int inDZ, int axisDX, int axisDZ,
                           int roadDepth) {}

    /**
     * Finds where village streets exit the wall and returns a GateDef for the best few.
     * No grid cells are marked; source arches are carved through the completed wall plan.
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
            int roadDepth = gateRoadDepth(nx, nz, tipx, tipz);
            gates.add(new GateDef(cwx, cwz, refY, -ddx, -ddz, axisDX, axisDZ, roadDepth));
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

    static int gateRoadDepth(int wallX, int wallZ, int roadTipX, int roadTipZ) {
        return Math.abs(wallX - roadTipX) + Math.abs(wallZ - roadTipZ);
    }

    static int inwardGateGradeLength(int roadDepth) {
        return Math.max(0, roadDepth - GATE_FLAT_APRON_DEPTH);
    }

    // ── Wall placement ─────────────────────────────────────────────────────────

    /**
     * A road may only create a gate where it reaches a straight wall face head-on.
     * This rejects corner and parallel-wall hits that would put the arch sideways
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

    static List<SourceWallBlock> sourceWallTemplate(byte[][] grid, int gx, int gz, int w, int d) {
        List<SourceWallBlock> blocks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < WALL_HEIGHT; dy++) {
                    blocks.add(new SourceWallBlock(new BlockPos(dx, dy, dz), SourceWallPart.MASONRY,
                            null, Half.BOTTOM, PLAN_PRIORITY_BODY));
                }
            }
        }

        boolean n = isWallCell(grid, gx, gz - 1, w, d);
        boolean s = isWallCell(grid, gx, gz + 1, w, d);
        boolean e = isWallCell(grid, gx + 1, gz, w, d);
        boolean west = isWallCell(grid, gx - 1, gz, w, d);
        boolean ne = isWallCell(grid, gx + 1, gz - 1, w, d);
        boolean nw = isWallCell(grid, gx - 1, gz - 1, w, d);
        boolean se = isWallCell(grid, gx + 1, gz + 1, w, d);
        boolean sw = isWallCell(grid, gx - 1, gz + 1, w, d);

        if (!ne && !n && !e) addSourceCorner(blocks, 1, -1);
        if (!nw && !n && !west) addSourceCorner(blocks, -1, -1);
        if (!se && !s && !e) addSourceCorner(blocks, 1, 1);
        if (!sw && !s && !west) addSourceCorner(blocks, -1, 1);

        if (!n && !ne && !nw) addSourceFace(blocks, 0, -2, Direction.EAST);
        if (!e && !se && !ne) addSourceFace(blocks, 2, 0, Direction.SOUTH);
        if (!s && !se && !sw) addSourceFace(blocks, 0, 2, Direction.EAST);
        if (!west && !nw && !sw) addSourceFace(blocks, -2, 0, Direction.SOUTH);
        return blocks;
    }

    static List<SourceWallBlock> sourceGateTemplate(int axisDX, int axisDZ, int inDX, int inDZ) {
        List<SourceWallBlock> blocks = new ArrayList<>();
        int walkY = WALL_HEIGHT;
        int archY = walkY - SOURCE_GATE_ARCH_DROP;

        for (int along : new int[]{-SOURCE_GATE_HALF_WIDTH, SOURCE_GATE_HALF_WIDTH}) {
            for (int depth = -SOURCE_GATE_DEPTH; depth <= SOURCE_GATE_DEPTH; depth++) {
                for (int y = 0; y <= archY; y++) {
                    addOriented(blocks, along, depth, y, axisDX, axisDZ, inDX, inDZ,
                            SourceWallPart.MASONRY, null, Half.BOTTOM, PLAN_PRIORITY_BODY);
                }
            }
        }
        for (int along = -SOURCE_GATE_HALF_WIDTH; along <= SOURCE_GATE_HALF_WIDTH; along++) {
            for (int depth = -SOURCE_GATE_DEPTH; depth <= SOURCE_GATE_DEPTH; depth++) {
                for (int y = archY + 1; y < walkY; y++) {
                    addOriented(blocks, along, depth, y, axisDX, axisDZ, inDX, inDZ,
                            SourceWallPart.MASONRY, null, Half.BOTTOM, PLAN_PRIORITY_BODY);
                }
            }
        }

        for (int along = -SOURCE_GATE_OPENING_HALF_WIDTH;
             along <= SOURCE_GATE_OPENING_HALF_WIDTH; along++) {
            addOriented(blocks, along, 0, archY, axisDX, axisDZ, inDX, inDZ,
                    SourceWallPart.ARCH_WALL, null, Half.BOTTOM, PLAN_PRIORITY_CORNER);
        }

        Direction axisFacing = horizontalDirection(axisDX, axisDZ);
        for (int along : new int[]{-SOURCE_GATE_OPENING_HALF_WIDTH, SOURCE_GATE_OPENING_HALF_WIDTH}) {
            Direction facing = along < 0 ? axisFacing.getOpposite() : axisFacing;
            for (int depth : new int[]{-SOURCE_GATE_DEPTH, SOURCE_GATE_DEPTH}) {
                addOriented(blocks, along, depth, archY, axisDX, axisDZ, inDX, inDZ,
                        SourceWallPart.STAIR, facing, Half.TOP, PLAN_PRIORITY_CORNER);
            }
        }

        for (int along = -SOURCE_GATE_HALF_WIDTH; along <= SOURCE_GATE_HALF_WIDTH; along++) {
            for (int depth : new int[]{-2, 2}) {
                addOriented(blocks, along, depth, walkY, axisDX, axisDZ, inDX, inDZ,
                        SourceWallPart.MASONRY, null, Half.BOTTOM, PLAN_PRIORITY_RAIL);
                addOriented(blocks, along, depth, walkY + 1, axisDX, axisDZ, inDX, inDZ,
                        SourceWallPart.STAIR, axisFacing, Half.BOTTOM, PLAN_PRIORITY_STAIR);
            }
        }
        return blocks;
    }

    private static void addSourceWallStamp(WallBuildPlan plan, int wx, int sy, int wz, int floorTo,
                                           BlockState masonry, BlockState stair,
                                           byte[][] grid, int gx, int gz, int w, int d) {
        for (SourceWallBlock block : sourceWallTemplate(grid, gx, gz, w, d)) {
            BlockPos offset = block.offset();
            plan.add(wx + offset.getX(), sy + offset.getY(), wz + offset.getZ(),
                    sourceState(block, masonry, masonry, stair), false, block.priority());
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = floorTo; y < sy; y++) {
                    plan.add(wx + dx, y, wz + dz, masonry, false, PLAN_PRIORITY_BODY);
                }
                plan.addFoundationColumn(wx + dx, floorTo - 1, wz + dz);
            }
        }
    }

    private static void addSourceCorner(List<SourceWallBlock> blocks, int dx, int dz) {
        int[][] offsets = {{dx * 2, dz * 2}, {dx * 2, dz}, {dx, dz * 2}};
        for (int[] offset : offsets) {
            blocks.add(new SourceWallBlock(new BlockPos(offset[0], WALL_HEIGHT, offset[1]),
                    SourceWallPart.MASONRY, null, Half.BOTTOM, PLAN_PRIORITY_CORNER));
            blocks.add(new SourceWallBlock(new BlockPos(offset[0], WALL_HEIGHT + 1, offset[1]),
                    SourceWallPart.MASONRY, null, Half.BOTTOM, PLAN_PRIORITY_CORNER));
        }
    }

    private static void addSourceFace(List<SourceWallBlock> blocks, int dx, int dz, Direction facing) {
        blocks.add(new SourceWallBlock(new BlockPos(dx, WALL_HEIGHT, dz), SourceWallPart.MASONRY,
                null, Half.BOTTOM, PLAN_PRIORITY_RAIL));
        blocks.add(new SourceWallBlock(new BlockPos(dx, WALL_HEIGHT + 1, dz), SourceWallPart.STAIR,
                facing, Half.BOTTOM, PLAN_PRIORITY_STAIR));
    }

    private static void addOriented(List<SourceWallBlock> blocks, int along, int depth, int y,
                                    int axisDX, int axisDZ, int inDX, int inDZ,
                                    SourceWallPart part, Direction facing, Half half, int priority) {
        blocks.add(new SourceWallBlock(
                new BlockPos(axisDX * along + inDX * depth, y, axisDZ * along + inDZ * depth),
                part, facing, half, priority));
    }

    private static boolean isWallCell(byte[][] grid, int x, int z, int w, int d) {
        return insideGrid(x, z, w, d) && grid[x][z] == WALL;
    }

    private static BlockState sourceState(SourceWallBlock block, BlockState masonry,
                                          BlockState archWall, BlockState stair) {
        return switch (block.part()) {
            case MASONRY -> masonry;
            case ARCH_WALL -> archWall;
            case STAIR -> stair.setValue(StairBlock.FACING, block.facing())
                    .setValue(StairBlock.HALF, block.half())
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        };
    }

    private static void applyWallPlan(ServerLevel level, WallBuildPlan plan, BlockState foundation) {
        for (Map.Entry<HorizontalPos, Integer> entry : plan.foundationStarts.entrySet()) {
            HorizontalPos pos = entry.getKey();
            fillDown(level, pos.x(), entry.getValue(), pos.z(), foundation);
        }
        for (Map.Entry<BlockPos, PlannedWallBlock> entry : plan.blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            PlannedWallBlock block = entry.getValue();
            if (block.force()) forceSet(level, pos.getX(), pos.getY(), pos.getZ(), block.state());
            else placeWallMat(level, pos.getX(), pos.getY(), pos.getZ(), block.state());
        }
    }

    private static void relocatePlanBlockers(ServerLevel level, WallBuildPlan plan, String feature) {
        AABB bounds = plan.bounds();
        if (bounds == null) return;
        List<LivingEntity> blockers = level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity.isAlive() && plan.intersects(entity.getBoundingBox()));
        for (LivingEntity blocker : blockers) {
            BlockPos safe = findPlanRelocation(level, blocker, plan);
            if (safe == null) {
                plan.removeIntersecting(blocker.getBoundingBox());
                WitcheryMod.LOGGER.info("[VillageWalls] Could not safely move {} out of {}; leaving its occupied blocks open.",
                        blocker.getType(), feature);
                continue;
            }
            blocker.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
            WitcheryMod.LOGGER.info("[VillageWalls] Moved {} out of {} to {},{},{} before placement.",
                    blocker.getType(), feature, safe.getX(), safe.getY(), safe.getZ());
        }
    }

    private static BlockPos findPlanRelocation(ServerLevel level, LivingEntity entity, WallBuildPlan plan) {
        BlockPos origin = entity.blockPosition();
        for (int radius = 1; radius <= WALL_ENTITY_RELOCATION_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = safeRelocationPos(level, origin.getX() + dx, origin.getZ() + dz, entity);
                    if (candidate == null) continue;
                    if (!isRelocationHeightAllowed(origin.getY(), candidate.getY())) continue;
                    AABB reservedSpace = entityBoxAt(entity, candidate.getX(), candidate.getY(), candidate.getZ())
                            .expandTowards(0.0D, -1.0D, 0.0D);
                    if (!plan.intersects(reservedSpace)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    static boolean isRelocationHeightAllowed(int originY, int candidateY) {
        return Math.abs(candidateY - originY) <= WALL_ENTITY_RELOCATION_VERTICAL_LIMIT;
    }

    private static boolean isInsideGateCrown(BlockPos candidate, List<GateDef> gates) {
        for (GateDef gate : gates) {
            int dx = candidate.getX() - gate.centerWX();
            int dz = candidate.getZ() - gate.centerWZ();
            int along = dx * gate.axisDX() + dz * gate.axisDZ();
            int depth = dx * gate.inDX() + dz * gate.inDZ();
            if (Math.abs(along) <= SOURCE_GATE_HALF_WIDTH && Math.abs(depth) <= 2) return true;
        }
        return false;
    }

    private static boolean isSpawnClear(ServerLevel level, BlockPos pos) {
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        return (feet.isAir() || feet.canBeReplaced()) && (head.isAir() || head.canBeReplaced());
    }

    private static boolean moveLivingEntitiesOutOfGateColumn(ServerLevel level, int x, int minY, int z, int maxY,
                                                              String feature) {
        AABB box = new AABB(x, minY, z, x + 1.0, maxY + 1.0, z + 1.0);
        List<LivingEntity> blockers = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        if (blockers.isEmpty()) return true;

        for (LivingEntity blocker : blockers) {
            BlockPos safe = findNearbyRelocation(level, x, z, blocker);
            if (safe == null) {
                WitcheryMod.LOGGER.info("[VillageWalls] Could not safely move {} out of {} column {},{}; skipping column.",
                        blocker.getType(), feature, x, z);
                return false;
            }
            blocker.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
            WitcheryMod.LOGGER.info("[VillageWalls] Moved {} from {} column {},{} to {},{},{} before placing blocks.",
                    blocker.getType(), feature, x, z,
                    safe.getX(), safe.getY(), safe.getZ());
        }

        return level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty();
    }

    private static BlockPos findNearbyRelocation(ServerLevel level, int x, int z, LivingEntity entity) {
        for (int radius = 1; radius <= WALL_ENTITY_RELOCATION_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = safeRelocationPos(level, x + dx, z + dz, entity);
                    if (candidate != null) return candidate;
                }
            }
        }
        return null;
    }

    private static void rescueCollidingLivingEntities(ServerLevel level, WallBuildPlan wallPlan,
                                                       int minX, int minZ, int maxX, int maxZ) {
        AABB villageEnvelope = new AABB(minX, MIN_BUILD_Y, minZ, maxX + 1.0, MAX_BUILD_Y + 1.0, maxZ + 1.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, villageEnvelope, LivingEntity::isAlive)) {
            if (level.noCollision(entity, entity.getBoundingBox())) continue;
            int x = entity.blockPosition().getX();
            int y = entity.blockPosition().getY();
            int z = entity.blockPosition().getZ();
            BlockPos safe = findPlanRelocation(level, entity, wallPlan);
            if (safe == null) {
                WitcheryMod.LOGGER.warn("[VillageWalls] Could not rescue colliding {} at {},{},{} after wall generation.",
                        entity.getType(), entity.getX(), entity.getY(), entity.getZ());
                continue;
            }
            entity.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
            WitcheryMod.LOGGER.info("[VillageWalls] Rescued colliding {} at {},{},{} to {},{},{} after wall generation.",
                    entity.getType(), x, y, z,
                    safe.getX(), safe.getY(), safe.getZ());
        }
    }

    private static BlockPos safeRelocationPos(ServerLevel level, int x, int z, LivingEntity entity) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return null;
        int y = groundHeight(level, x, z);
        BlockPos stand = new BlockPos(x, y, z);
        if (!level.getBlockState(stand.below()).isSolid()) return null;
        if (!isRelocationSpace(level, stand) || !isRelocationSpace(level, stand.above())) return null;

        AABB targetBox = entityBoxAt(entity, x, y, z);
        if (!level.getEntitiesOfClass(LivingEntity.class, targetBox,
                other -> other.isAlive() && other != entity).isEmpty())
            return null;
        return level.noCollision(entity, targetBox) ? stand : null;
    }

    private static AABB entityBoxAt(LivingEntity entity, int x, int y, int z) {
        double halfWidth = Math.max(0.3D, entity.getBbWidth() * 0.5D);
        double height = Math.max(1.0D, entity.getBbHeight());
        double cx = x + 0.5D;
        double cz = z + 0.5D;
        return new AABB(cx - halfWidth, y, cz - halfWidth,
                cx + halfWidth, y + height, cz + halfWidth);
    }

    private static boolean isRelocationSpace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.liquid() && (state.isAir() || state.canBeReplaced());
    }

    /**
     * Rebuilds the source wall's integrated gate: a 3-wide, 3-deep tunnel with mirrored
     * upside-down stair shoulders, a narrow wall-block beam, two roof courses, and the same
     * recessed patrol deck as the curtain. Modern path grading and terrain support remain.
     */
    private static void buildSourceGate(ServerLevel level, GateDef gate, BlockState masonry,
                                        BlockState archWall, BlockState stair, BlockState pathBlock,
                                        boolean desert, boolean spawnGuards) {
        int refY = gate.refY();
        int walkY = refY + WALL_HEIGHT;
        BlockState pathSupport = pathSupportMaterial(level, gate.centerWX(), gate.centerWZ(), desert);

        WallBuildPlan gatePlan = new WallBuildPlan();
        for (SourceWallBlock block : sourceGateTemplate(
                gate.axisDX(), gate.axisDZ(), gate.inDX(), gate.inDZ())) {
            BlockPos offset = block.offset();
            gatePlan.add(gate.centerWX() + offset.getX(), refY + offset.getY(), gate.centerWZ() + offset.getZ(),
                    sourceState(block, masonry, archWall, stair), true, block.priority());
        }
        relocatePlanBlockers(level, gatePlan, "gate arch");

        clearSourceGateFootprint(level, gate);
        buildFlatGateFloor(level, gate, pathBlock, pathSupport);
        applyWallPlan(level, gatePlan, masonry);

        for (int along : new int[]{-SOURCE_GATE_HALF_WIDTH, SOURCE_GATE_HALF_WIDTH}) {
            for (int depth = -SOURCE_GATE_DEPTH; depth <= SOURCE_GATE_DEPTH; depth++) {
                int x = gate.centerWX() + gate.axisDX() * along + gate.inDX() * depth;
                int z = gate.centerWZ() + gate.axisDZ() * along + gate.inDZ() * depth;
                fillDown(level, x, refY - 1, z, masonry);
            }
        }

        int outDX = -gate.inDX();
        int outDZ = -gate.inDZ();
        for (int along = -SOURCE_GATE_OPENING_HALF_WIDTH;
             along <= SOURCE_GATE_OPENING_HALF_WIDTH; along++) {
            int x = gate.centerWX() + gate.axisDX() * along;
            int z = gate.centerWZ() + gate.axisDZ() * along;
            gradePath(level, x + outDX * GATE_FLAT_APRON_DEPTH, z + outDZ * GATE_FLAT_APRON_DEPTH,
                    outDX, outDZ, refY - 1, GATE_PATH_OUTWARD_MAX, false, false,
                    pathBlock, pathSupport, desert);
            gradePath(level, x + gate.inDX() * GATE_FLAT_APRON_DEPTH,
                    z + gate.inDZ() * GATE_FLAT_APRON_DEPTH,
                    gate.inDX(), gate.inDZ(), refY - 1, inwardGateGradeLength(gate.roadDepth()),
                    true, true, pathBlock, pathSupport, desert);
        }

        if (spawnGuards) {
            placeGateLanterns(level, gate, archWall);
            BlockPos guardPos = new BlockPos(gate.centerWX(), walkY, gate.centerWZ());
            if (level.getBlockState(guardPos.below()).isSolid() && isSpawnClear(level, guardPos)) {
                spawnGuard(level, guardPos);
            }
        }
    }

    /** Keeps the tunnel and one apron block on both sides level with clear player headroom. */
    private static void buildFlatGateFloor(ServerLevel level, GateDef gate,
                                           BlockState pathBlock, BlockState pathSupport) {
        int floorY = gate.refY() - 1;
        for (int along = -SOURCE_GATE_OPENING_HALF_WIDTH;
             along <= SOURCE_GATE_OPENING_HALF_WIDTH; along++) {
            for (int depth = -GATE_FLAT_APRON_DEPTH; depth <= GATE_FLAT_APRON_DEPTH; depth++) {
                int x = gate.centerWX() + gate.axisDX() * along + gate.inDX() * depth;
                int z = gate.centerWZ() + gate.axisDZ() * along + gate.inDZ() * depth;
                if (!moveLivingEntitiesOutOfGateColumn(level, x, floorY, z,
                        gate.refY() + 4, "flat gate floor")) {
                    continue;
                }
                forceSet(level, x, floorY, z, pathBlock);
                fillDown(level, x, floorY - 1, z, pathSupport);
                for (int y = gate.refY(); y <= gate.refY() + 4; y++) {
                    forceSet(level, x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void clearSourceGateFootprint(ServerLevel level, GateDef gate) {
        int walkY = gate.refY() + WALL_HEIGHT;
        for (int along = -SOURCE_GATE_HALF_WIDTH; along <= SOURCE_GATE_HALF_WIDTH; along++) {
            for (int depth = -2; depth <= 2; depth++) {
                int x = gate.centerWX() + gate.axisDX() * along + gate.inDX() * depth;
                int z = gate.centerWZ() + gate.axisDZ() * along + gate.inDZ() * depth;
                clearTouchedVegetation(level, x, gate.refY() - 1, z,
                        WALL_HEIGHT + GATE_CLEAR_ABOVE_WALL, TREE_CLEAR_WALL_TOUCH_RADIUS);
                int minY = Math.abs(depth) <= SOURCE_GATE_DEPTH ? gate.refY() : walkY;
                for (int y = minY; y <= walkY + 1; y++) {
                    forceSet(level, x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void placeGateLanterns(ServerLevel level, GateDef g, BlockState bracket) {
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        int outsideDX = -g.inDX();
        int outsideDZ = -g.inDZ();
        Direction towardWall = horizontalDirection(g.inDX(), g.inDZ());
        int bracketY = g.refY() + 4;
        int lanternY = bracketY - 1;
        for (int o : new int[]{-2, 2}) {
            int x = g.centerWX() + g.axisDX() * o + outsideDX * 2;
            int z = g.centerWZ() + g.axisDZ() * o + outsideDZ * 2;
            forceSet(level, x, bracketY, z, bracketConnectedToward(bracket, towardWall));
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
            if (!moveLivingEntitiesOutOfGateColumn(level, ix, Math.min(floorY, natural), iz,
                    Math.max(floorY, natural) + 4, "gate path")) break;
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

    static BlockState wallMaterial(VillageBuildingMaterials.VillageType villageType) {
        // One dominant masonry per village family, matching the source wall's solid mass.
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.TAIGA)
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** Narrow wall-block beam in the source arch, also used by its lantern brackets. */
    static BlockState archMaterial(VillageBuildingMaterials.VillageType villageType) {
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.SANDSTONE_WALL.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.RED_SANDSTONE_WALL.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.TAIGA)
            return Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState();
        return Blocks.COBBLESTONE_WALL.defaultBlockState();
    }

    /** Biome-adaptive stair caps for the crown rails and mirrored arch shoulders. */
    static BlockState stairMaterial(VillageBuildingMaterials.VillageType villageType) {
        if (villageType == VillageBuildingMaterials.VillageType.DESERT)
            return Blocks.SANDSTONE_STAIRS.defaultBlockState();
        if (villageType == VillageBuildingMaterials.VillageType.SAVANNA)
            return Blocks.RED_SANDSTONE_STAIRS.defaultBlockState();
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState();
    }

    // ── Block helpers ──────────────────────────────────────────────────────────

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
