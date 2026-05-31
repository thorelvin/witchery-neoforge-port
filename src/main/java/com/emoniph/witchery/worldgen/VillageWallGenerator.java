package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.server.level.ServerLevel;

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
 *  8. Spawn guards on wall-top every ~200 wall blocks.
 */
public final class VillageWallGenerator {

    private static final int WALL_HEIGHT   = 8;   // blocks above surface
    // PADDING must exceed SMOOTH + BLOCK (see step 3c) so the coarse-hull blocks can never
    // reach the grid border; if the INTERIOR touches the edge the flood-fill cannot mark
    // OUTSIDE there and that side is left unwalled.
    private static final int PADDING       = 18;  // extra space outside village edge
    private static final int MAX_GRID      = 260; // abort if village is too large
    private static final int GAP_FILL      = 4;   // max gap to bridge between structures
    private static final int GUARD_SPACING = 200; // wall blocks between guard spawns
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

    public static void generate(ServerLevel level, StructureStart start) {
        List<StructurePiece> pieces = start.getPieces();
        WitcheryMod.LOGGER.info("[VillageWalls] generate() called: {} pieces, bb={}",
                pieces.size(), start.getBoundingBox());
        if (pieces.isEmpty()) return;

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
            return;
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

        // 5b. Orthogonalise diagonal WALL adjacencies — SINGLE pass only.
        //     With the coarse hull, diagonal WALL pairs are rare and isolated (they occur
        //     only where two coarse blocks touch at a corner), so one pass connects them
        //     with a 90° L by converting the INTERIOR-side shared corner to WALL. A single
        //     pass cannot thicken straight runs (the previous multi-pass version grew
        //     diagonal staircases into wide terraced bands).
        {
            byte[][] snap = copyGrid(grid, w, d);
            for (int x = 1; x < w - 1; x++) {
                for (int z = 1; z < d - 1; z++) {
                    if (snap[x][z] != WALL) continue;
                    for (int dx : new int[]{-1, 1}) {
                        for (int dz : new int[]{-1, 1}) {
                            if (snap[x + dx][z + dz] != WALL) continue; // diagonal WALL pair
                            if (snap[x + dx][z] == INTERIOR) grid[x + dx][z] = WALL;
                            else if (snap[x][z + dz] == INTERIOR) grid[x][z + dz] = WALL;
                        }
                    }
                }
            }
        }

        // 6. Remove small disconnected wall loops — keep only the largest connected ring.
        //    Gap-fill sometimes creates separate interior zones that each get their own wall;
        //    those internal loops must be removed before placement.
        pruneSmallWallLoops(grid, w, d);

        // 7. Gate detection: scan INTERIOR cells that directly border the wall for DIRT_PATH.
        //    Roads are inside the village (1–N cells inward from the wall), so scanning the
        //    wall cell itself rarely finds them — scanning its interior neighbours does.
        detectGates(level, grid, originX, originZ, w, d);
        expandGates(grid, w, d);

        // 7. Biome-adaptive materials
        int cx = minX + (maxX - minX) / 2;
        int cz = minZ + (maxZ - minZ) / 2;
        BlockState wallBlock   = wallMaterial(level, cx, cz);
        BlockState brickBlock  = brickMaterial(level, cx, cz);
        BlockState merlonBlock = merlonMaterial(level, cx, cz);

        // 8. Pre-sample ALL terrain heights before placing any blocks.
        //    placeThickenBlock wrote cobblestone at neighbors, which updated their heightmap;
        //    when those cells were later processed their base Y was wrong — hence stacking towers.
        //    Sampling everything first prevents that cascade entirely.
        int minBuild = -64; // 1.21 minimum build height
        short[][] heights = new short[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != WALL && grid[x][z] != GATE) continue;
                int wx = originX + x, wz = originZ + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int h = groundHeight(level, wx, wz); // tree-corrected ground, not treetop
                heights[x][z] = (short) Math.max(h, minBuild + 5);
            }
        }

        // 9a. Pre-clear vegetation/trees in every wall and gate column so logs and
        //     leaves don't block wall placement (setBlock skips solid non-replaceable blocks).
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != WALL && grid[x][z] != GATE) continue;
                int wx = originX + x, wz = originZ + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int sy = heights[x][z];
                if (sy == 0) continue;
                for (int dy = 0; dy < WALL_HEIGHT + 5; dy++) {
                    BlockPos p = new BlockPos(wx, sy + dy, wz);
                    BlockState bs = level.getBlockState(p);
                    if (!bs.isAir() && isNaturalVegetation(bs))
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // 9. Place walls and gates using pre-sampled heights
        int guardCounter = 0;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                int wx = originX + x, wz = originZ + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int sy = heights[x][z];
                if (sy == 0) continue; // unloaded, skip

                if (grid[x][z] == WALL) {
                    placeWallColumn(level, wx, sy, wz, wallBlock, brickBlock, merlonBlock, grid, x, z, w, d);
                    if (++guardCounter >= GUARD_SPACING) {
                        spawnGuard(level, wx, wz, sy);
                        guardCounter = 0;
                    }
                } else if (grid[x][z] == GATE) {
                    placeGatePillar(level, wx, sy, wz, wallBlock, brickBlock, merlonBlock, grid, x, z, w, d);
                }
            }
        }

        WitcheryMod.LOGGER.info("[VillageWalls] Generated walls for village at {},{} ({}x{})",
                cx, cz, w, d);
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

    private static long pack(int x, int z) { return (long)x << 20 | z; }

    private static boolean isNaturalVegetation(BlockState state) {
        return state.is(BlockTags.LEAVES)
            || state.is(BlockTags.LOGS)
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.SAPLINGS)
            || state.canBeReplaced();
    }

    // ── Gate detection ─────────────────────────────────────────────────────────

    /**
     * Marks wall cells as GATE where a village road exits through the perimeter.
     *
     * "Detect a path leaving the area, extend it through the wall":
     *
     * Phase 1 — precompute which INTERIOR cells contain a DIRT_PATH road block
     * at the surface.  Only the exact surface block is checked (not a deep vertical
     * window) so natural gravel/dirt deposits underground are NOT mistaken for roads.
     * DIRT_PATH is the village street block in plains/savanna/taiga/snowy villages.
     *
     * Phase 2 — for each road cell that is the TIP of a run in a cardinal
     * direction (road exists behind it in that direction, no road ahead), trace
     * forward until the nearest WALL cell and mark it GATE.  This captures
     * exactly the road segments that EXIT the village: roads running parallel to
     * the wall do not have a tip pointing toward it, so they never trigger gates
     * at wrong positions.
     */
    private static void detectGates(ServerLevel level, byte[][] grid,
                                     int ox, int oz, int w, int d) {
        // Phase 1: locate DIRT_PATH road blocks at the surface of INTERIOR cells
        boolean[][] road = new boolean[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != INTERIOR) continue;
                int wx = ox + x, wz = oz + z;
                if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
                int gy = groundHeight(level, wx, wz);
                // The surface block is at gy-1 (groundHeight returns the cell above ground).
                if (level.getBlockState(new BlockPos(wx, gy - 1, wz)).is(Blocks.DIRT_PATH)) {
                    road[x][z] = true;
                }
            }
        }

        // Phase 2: find the END of each MAIN STREET and trace it through the wall.
        //
        // A gate must come from an actual street exiting the village, not from every road
        // cell. The road network has many short fragments and plaza/junction edges; treating
        // each as a tip marks gates all around the wall. So a cell only qualifies as a street
        // end in direction (dir) when:
        //   • a long straight run of road (>= MIN_RUN) leads up to it from behind (-dir),
        //     bridging small dirt/gravel gaps (ROAD_GAP) — i.e. it's the end of a real street;
        //   • the street does NOT continue ahead (no road within ROAD_GAP in +dir).
        // The street's trajectory is then extended straight ahead through the interior to the
        // nearest WALL cell, which becomes the gate.
        final int ROAD_GAP = 3;
        final int MIN_RUN  = 5;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int x = 1; x < w - 1; x++) {
            for (int z = 1; z < d - 1; z++) {
                if (!road[x][z]) continue;
                for (int[] dir : dirs) {
                    // Long straight street leading up to this cell from behind?
                    if (roadRun(road, x, z, -dir[0], -dir[1], w, d, ROAD_GAP, 40) < MIN_RUN) continue;
                    // Street ends here (does not continue ahead)?
                    if (roadWithin(road, x, z, dir[0], dir[1], w, d, ROAD_GAP)) continue;

                    // Extend the street straight ahead through the interior to the wall.
                    for (int depth = 1; depth < Math.max(w, d); depth++) {
                        int nx = x + dir[0] * depth, nz = z + dir[1] * depth;
                        if (nx < 0 || nz < 0 || nx >= w || nz >= d) break;
                        if (grid[nx][nz] == INTERIOR) continue; // still inside, keep going
                        if (grid[nx][nz] == WALL) { grid[nx][nz] = GATE; }
                        break; // WALL marked (or EMPTY — no wall this direction)
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

    /** Expand each GATE cell ±2 cells along the wall run for a 5-wide gate. */
    private static void expandGates(byte[][] grid, int w, int d) {
        byte[][] copy = copyGrid(grid, w, d);
        for (int x = 2; x < w-2; x++) {
            for (int z = 2; z < d-2; z++) {
                if (copy[x][z] != GATE) continue;
                // Determine wall axis from adjacent WALL neighbors
                boolean wallEW = copy[x-1][z] == WALL || copy[x+1][z] == WALL ||
                                 copy[x-1][z] == GATE || copy[x+1][z] == GATE;
                boolean wallNS = copy[x][z-1] == WALL || copy[x][z+1] == WALL ||
                                 copy[x][z-1] == GATE || copy[x][z+1] == GATE;

                if (wallEW) { // wall runs E-W, gate opens N-S: expand along X
                    for (int dx = -2; dx <= 2; dx++) {
                        int nx = x + dx;
                        if (nx >= 0 && nx < w && grid[nx][z] == WALL) grid[nx][z] = GATE;
                    }
                } else if (wallNS) { // wall runs N-S: expand along Z
                    for (int dz = -2; dz <= 2; dz++) {
                        int nz = z + dz;
                        if (nz >= 0 && nz < d && grid[x][nz] == WALL) grid[x][nz] = GATE;
                    }
                }
            }
        }
    }

    // ── Wall placement ─────────────────────────────────────────────────────────

    /**
     * One wall column:
     *  - Stone brick base (2 courses)
     *  - Cobblestone main shaft
     *  - Stone brick cap
     *  - 1-high stone brick wall merlon every 2 blocks (classic castle cadence)
     *  - Corner bonus: +2 height + 2-high merlon (tower effect)
     */
    private static void placeWallColumn(ServerLevel level, int wx, int sy, int wz,
                                         BlockState wall, BlockState brick, BlockState merlon,
                                         byte[][] grid, int gx, int gz, int w, int d) {
        boolean corner = isCorner(grid, gx, gz, w, d);
        int extra = corner ? 2 : 0;
        int top = sy + WALL_HEIGHT + extra;

        // Base: 2 courses of stone brick
        setBlock(level, wx, sy,     wz, brick);
        setBlock(level, wx, sy + 1, wz, brick);
        // Main cobblestone shaft
        for (int dy = 2; dy < WALL_HEIGHT - 1 + extra; dy++)
            setBlock(level, wx, sy + dy, wz, wall);
        // Cap course: stone brick
        setBlock(level, wx, top - 1, wz, brick);

        // Merlons: 1-high stone brick wall every 2 blocks (period=2, classic castle).
        // Corners get 2-high merlons to emphasise the tower.
        int phase = Math.floorMod(wx + wz, 2);
        if (phase == 0) {
            setBlock(level, wx, top, wz, merlon);
            if (corner) setBlock(level, wx, top + 1, wz, merlon);
        }

        fillDown(level, wx, sy - 1, wz, wall);
    }

    /** True when this wall cell sits at a 90° bend (has wall in both N/S and E/W axes). */
    private static boolean isCorner(byte[][] grid, int gx, int gz, int w, int d) {
        boolean n = gz > 0   && (grid[gx][gz-1] == WALL || grid[gx][gz-1] == GATE);
        boolean s = gz < d-1 && (grid[gx][gz+1] == WALL || grid[gx][gz+1] == GATE);
        boolean e = gx < w-1 && (grid[gx+1][gz] == WALL || grid[gx+1][gz] == GATE);
        boolean ww = gx > 0  && (grid[gx-1][gz] == WALL || grid[gx-1][gz] == GATE);
        return (n || s) && (e || ww);
    }

    /**
     * Gate pillar column:
     *  - Full-height stone brick pillar at edge positions
     *  - Cobblestone wall block finial on top
     *  - Open (air) at middle gate cells
     */
    private static void placeGatePillar(ServerLevel level, int wx, int sy, int wz,
                                          BlockState wall, BlockState brick, BlockState merlon,
                                          byte[][] grid, int gx, int gz, int w, int d) {
        // Determine the wall axis (the direction the gate RUN extends along).
        boolean wallRunsEW = (gx > 0 && (grid[gx-1][gz] == WALL || grid[gx-1][gz] == GATE))
                          || (gx < w-1 && (grid[gx+1][gz] == WALL || grid[gx+1][gz] == GATE));

        // A gate is a 1-cell-deep run of GATE cells along the wall axis. The two END cells
        // of that run are pillars; the middle cells are the opening. "Edge" therefore means
        // an END of the run along the wall axis — NOT a generic non-GATE neighbour (the
        // perpendicular inward/outward neighbours are never GATE, so the old all-direction
        // check made every gate cell an edge and the opening was never carved).
        boolean isEdge;
        if (wallRunsEW) {
            boolean gateW = gx > 0   && grid[gx-1][gz] == GATE;
            boolean gateE = gx < w-1 && grid[gx+1][gz] == GATE;
            isEdge = !(gateW && gateE);
        } else {
            boolean gateN = gz > 0   && grid[gx][gz-1] == GATE;
            boolean gateS = gz < d-1 && grid[gx][gz+1] == GATE;
            isEdge = !(gateN && gateS);
        }

        // Inward direction (toward the village interior), perpendicular to the wall axis.
        int inDX = 0, inDZ = 0;
        if (wallRunsEW) {
            if (gz > 0 && (grid[gx][gz-1] == INTERIOR || grid[gx][gz-1] == GATE)) inDZ = -1;
            else if (gz < d-1 && (grid[gx][gz+1] == INTERIOR || grid[gx][gz+1] == GATE)) inDZ = 1;
        } else {
            if (gx > 0 && (grid[gx-1][gz] == INTERIOR || grid[gx-1][gz] == GATE)) inDX = -1;
            else if (gx < w-1 && (grid[gx+1][gz] == INTERIOR || grid[gx+1][gz] == GATE)) inDX = 1;
        }

        if (!isEdge) {
            // Middle of gate — 3-block-deep passage + arch overhead.
            Direction facing = gateWallFacing(grid, gx, gz, w, d);
            BlockState fenceGate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                    .setValue(FenceGateBlock.FACING, facing);
            for (int depth = 0; depth < 3; depth++) {
                int dwx = wx + inDX * depth, dwz = wz + inDZ * depth;
                // Clear 5-block passage
                for (int dy = 0; dy < 5; dy++) {
                    BlockPos p = new BlockPos(dwx, sy + dy, dwz);
                    if (!level.getBlockState(p).isAir())
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
                // Arch/tunnel ceiling
                for (int dy = 5; dy < WALL_HEIGHT; dy++)
                    setBlock(level, dwx, sy + dy, dwz, brick);
            }
            // Fence gate at outer face only
            level.setBlock(new BlockPos(wx, sy, wz), fenceGate, 3);

            // Extend the path through the gate: from a few cells inside, through the
            // 3-deep passage, to several cells outside — so the village road visibly
            // exits the wall. Outward = opposite of inward.
            int oDX = -inDX, oDZ = -inDZ;
            for (int t = 1; t <= 5; t++)            // outside the wall
                layPath(level, wx + oDX * t, wz + oDZ * t);
            for (int depth = 0; depth < 3; depth++) // passage floor (inside)
                layPath(level, wx + inDX * depth, wz + inDZ * depth);
            for (int t = 3; t <= 5; t++)            // a little further inside to meet the road
                layPath(level, wx + inDX * t, wz + inDZ * t);
            return;
        }

        // Pillar — 3 blocks deep into the interior
        for (int depth = 0; depth < 3; depth++) {
            int dwx = wx + inDX * depth, dwz = wz + inDZ * depth;
            for (int dy = 0; dy < WALL_HEIGHT + 1; dy++)
                setBlock(level, dwx, sy + dy, dwz, brick);
            setBlock(level, dwx, sy + WALL_HEIGHT + 1, dwz, depth == 0 ? merlon : brick);
            if (depth == 0) fillDown(level, dwx, sy - 1, dwz, wall);
        }
    }

    /**
     * Returns the fence gate FACING direction based on the wall axis.
     * If the gate run extends E/W (wall runs E-W), traffic flows N/S → face NORTH.
     * If the gate run extends N/S (wall runs N-S), traffic flows E/W → face EAST.
     */
    private static Direction gateWallFacing(byte[][] grid, int gx, int gz, int w, int d) {
        boolean hasEW = (gx > 0 && (grid[gx-1][gz] == GATE || grid[gx-1][gz] == WALL))
                     || (gx < w-1 && (grid[gx+1][gz] == GATE || grid[gx+1][gz] == WALL));
        return hasEW ? Direction.NORTH : Direction.EAST;
    }

    // ── Guard spawning ─────────────────────────────────────────────────────────

    private static void spawnGuard(ServerLevel level, int wx, int wz, int presampled_sy) {
        int sy = presampled_sy + WALL_HEIGHT;
        BlockPos pos = new BlockPos(wx, sy, wz);
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

    // ── Biome materials ────────────────────────────────────────────────────────

    private static BlockState wallMaterial(ServerLevel level, int x, int z) {
        var biome = level.getBiome(new BlockPos(x, 64, z));
        if (biome.is(BiomeTags.IS_BADLANDS) || biome.value().getBaseTemperature() > 1.5f)
            return Blocks.SANDSTONE.defaultBlockState();
        if (biome.is(BiomeTags.IS_TAIGA))
            return Blocks.COBBLESTONE.defaultBlockState();
        if (biome.is(BiomeTags.IS_SAVANNA))
            return Blocks.COBBLESTONE.defaultBlockState();
        if (biome.is(BiomeTags.IS_FOREST))
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    private static BlockState brickMaterial(ServerLevel level, int x, int z) {
        var biome = level.getBiome(new BlockPos(x, 64, z));
        if (biome.is(BiomeTags.IS_BADLANDS) || biome.value().getBaseTemperature() > 1.5f)
            return Blocks.CHISELED_SANDSTONE.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** Battlement / merlon material — biome-adaptive wall-block variant. */
    private static BlockState merlonMaterial(ServerLevel level, int x, int z) {
        var biome = level.getBiome(new BlockPos(x, 64, z));
        if (biome.is(BiomeTags.IS_BADLANDS) || biome.value().getBaseTemperature() > 1.5f)
            return Blocks.SANDSTONE_WALL.defaultBlockState();
        if (biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_SAVANNA))
            return Blocks.COBBLESTONE_WALL.defaultBlockState();
        return Blocks.STONE_BRICK_WALL.defaultBlockState();
    }

    // ── Block helpers ──────────────────────────────────────────────────────────

    private static void setBlock(ServerLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return;
        BlockState existing = level.getBlockState(pos);
        // Replace air, liquids, replaceable decoration, and natural vegetation.
        // Logs and leaves are solid+non-replaceable in 1.21 but must be cleared for walls.
        if (!existing.isAir() && !existing.liquid() && !existing.canBeReplaced()
                && !existing.is(BlockTags.LOGS) && !existing.is(BlockTags.LEAVES)) return;
        level.setBlock(pos, state, 3);
    }

    private static void fillDown(ServerLevel level, int x, int startY, int z, BlockState block) {
        for (int y = startY; y >= startY - 12; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) break;
            if (level.getBlockState(pos).isSolid()) break;
            level.setBlock(pos, block, 3);
        }
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

    /**
     * Lays a DIRT_PATH block at the ground surface of a column and clears 3 blocks
     * of headroom above it (vegetation / replaceable only). Used to extend the
     * village road out through a gate so the path visibly leaves the wall.
     */
    private static void layPath(ServerLevel level, int wx, int wz) {
        if (level.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) return;
        int gy = groundHeight(level, wx, wz);
        level.setBlock(new BlockPos(wx, gy - 1, wz), Blocks.DIRT_PATH.defaultBlockState(), 3);
        for (int dy = 0; dy < 3; dy++) {
            BlockPos p = new BlockPos(wx, gy + dy, wz);
            BlockState bs = level.getBlockState(p);
            if (!bs.isAir() && (isNaturalVegetation(bs) || bs.canBeReplaced()))
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
