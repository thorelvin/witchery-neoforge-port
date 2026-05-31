package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.server.level.ServerLevel;
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
 *  8. Spawn guards on wall-top every ~200 wall blocks.
 */
public final class VillageWallGenerator {

    private static final int WALL_HEIGHT   = 6;   // blocks above surface
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

        // 7. Biome-adaptive materials
        int cx = minX + (maxX - minX) / 2;
        int cz = minZ + (maxZ - minZ) / 2;
        BlockState wallBlock   = wallMaterial(level, cx, cz);
        BlockState brickBlock  = brickMaterial(level, cx, cz);
        BlockState merlonBlock = merlonMaterial(level, cx, cz);

        // 7b. Detect gates as independent definitions (built after the wall, overriding it).
        //     No GATE cells are marked in the grid — the wall ring is solid and each gatehouse
        //     overrides whatever it covers. Road/path material is biome-aware (desert villages
        //     pave with smooth sandstone, not dirt path).
        boolean desert = isDesertLike(level, cx, cz);
        BlockState pathBlock = desert ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
                                      : Blocks.DIRT_PATH.defaultBlockState();
        List<GateDef> gates = detectGates(level, grid, originX, originZ, w, d, desert);

        // 8. Pre-sample wall base heights (water-aware). Walls follow terrain.
        int minBuild = -64; // 1.21 minimum build height
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

        // 9a. Pre-clear vegetation/trees in every wall column.
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (grid[x][z] != WALL) continue;
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

                placeWallColumn(level, wx, sy, wz, wallBlock, brickBlock, merlonBlock, floorTo, grid, x, z, w, d);
                if (++guardCounter >= GUARD_SPACING) {
                    spawnGuard(level, wx, wz, sy);
                    guardCounter = 0;
                }
            }
        }

        // 10. Build gatehouses — independent locked 7×3 models that override the wall.
        for (GateDef g : gates) buildGatehouse(level, g, brickBlock, pathBlock, desert);

        WitcheryMod.LOGGER.info("[VillageWalls] Generated walls for village at {},{} ({}x{}), {} gate(s).",
                cx, cz, w, d, gates.size());
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
     *          gate over water or without land on both sides; refY = median path height.
     */
    private static List<GateDef> detectGates(ServerLevel level, byte[][] grid,
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
                        if (grid[nx][nz] == WALL) candidates.add(new int[]{nx, nz, run, dir[0], dir[1], x, z});
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

            int refY = pathCorridorAvgY(level, cwx, cwz, ddx, ddz, desert);
            if (refY < 0) refY = wallBaseHeight(level, cwx, cwz);

            int axisDX = (ddx != 0) ? 0 : 1, axisDZ = (ddx != 0) ? 1 : 0; // along the wall
            gates.add(new GateDef(cwx, cwz, refY, -ddx, -ddz, axisDX, axisDZ));
            chosen.add(c);
            if (gates.size() >= MAX_GATES) break;
        }
        WitcheryMod.LOGGER.info("[VillageWalls] {} gate candidate(s) -> {} gate(s).",
                candidates.size(), gates.size());
        return gates;
    }

    /**
     * Average ground height along the gate's path corridor: from a few cells outside the wall
     * (-dir) inward (-dir·t) until the existing village road is reached. This is the ground the
     * gatehouse and its path actually sit on, so averaging it places the gate at a height that
     * the path can ramp to from both sides — instead of borrowing a far-away road's elevation.
     */
    private static int pathCorridorAvgY(ServerLevel level, int cwx, int cwz,
                                         int ddx, int ddz, boolean desert) {
        int idx = -ddx, idz = -ddz; // inward (toward the village)
        long sum = 0; int n = 0;
        for (int t = -4; t <= 40; t++) { // t<0 = outside the wall, t>0 = inside
            int ix = cwx + idx * t, iz = cwz + idz * t;
            if (level.getChunkSource().getChunkNow(ix >> 4, iz >> 4) == null) continue;
            int gy = groundHeight(level, ix, iz);
            sum += gy; n++;
            if (t > 0 && isVillageRoad(level.getBlockState(new BlockPos(ix, gy - 1, iz)), desert)) break;
        }
        return n > 0 ? (int) (sum / n) : -1;
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
    private static void placeWallColumn(ServerLevel level, int wx, int sy, int wz,
                                         BlockState wall, BlockState brick, BlockState merlon,
                                         int floorTo, byte[][] grid, int gx, int gz, int w, int d) {
        boolean corner = isCorner(grid, gx, gz, w, d);
        int extra = corner ? 2 : 0;
        int top = sy + WALL_HEIGHT + extra;
        if (hasLivingEntityInColumn(level, wx, Math.min(sy, floorTo), wz, top + 1)) return;

        // Base: 2 courses of stone brick (stone if submerged)
        placeWallMat(level, wx, sy,     wz, brick);
        placeWallMat(level, wx, sy + 1, wz, brick);
        // Main shaft (stone if submerged)
        for (int dy = 2; dy < WALL_HEIGHT - 1 + extra; dy++)
            placeWallMat(level, wx, sy + dy, wz, wall);
        // Cap course: stone brick
        setBlock(level, wx, top - 1, wz, brick);

        // Merlons: 1-high stone brick wall every 2 blocks (period=2, classic castle).
        int phase = Math.floorMod(wx + wz, 2);
        if (phase == 0) {
            setBlock(level, wx, top, wz, merlon);
            if (corner) setBlock(level, wx, top + 1, wz, merlon);
        }

        // Extend the curtain down to the lowest neighbour so cliff steps stay connected
        // (stone wherever submerged, via placeWallMat).
        for (int y = sy - 1; y >= floorTo; y--) placeWallMat(level, wx, y, wz, wall);
        fillDown(level, wx, Math.min(sy, floorTo) - 1, wz, brick);
    }

    /** True when this wall cell sits at a 90° bend (has wall in both N/S and E/W axes). */
    private static boolean isCorner(byte[][] grid, int gx, int gz, int w, int d) {
        boolean n = gz > 0   && (grid[gx][gz-1] == WALL || grid[gx][gz-1] == GATE);
        boolean s = gz < d-1 && (grid[gx][gz+1] == WALL || grid[gx][gz+1] == GATE);
        boolean e = gx < w-1 && (grid[gx+1][gz] == WALL || grid[gx+1][gz] == GATE);
        boolean ww = gx > 0  && (grid[gx-1][gz] == WALL || grid[gx-1][gz] == GATE);
        return (n || s) && (e || ww);
    }

    private static boolean hasLivingEntityInColumn(ServerLevel level, int x, int minY, int z, int maxY) {
        AABB box = new AABB(x, minY, z, x + 1.0, maxY + 1.0, z + 1.0);
        return !level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty();
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
    private static void buildGatehouse(ServerLevel level, GateDef g, BlockState brick,
                                        BlockState pathBlock, boolean desert) {
        BlockState gateCap = brick.is(Blocks.CHISELED_SANDSTONE)
                ? Blocks.SANDSTONE_WALL.defaultBlockState()
                : Blocks.STONE_BRICK_WALL.defaultBlockState();
        int refY = g.refY();

        // Depth -1..+1 centres the 3-deep gatehouse on the wall line, so the wall meets the
        // gate at the middle of its sides (one block of the gate sits outside the wall).
        for (int o = -3; o <= 3; o++) {
            int bx = g.centerWX() + g.axisDX() * o, bz = g.centerWZ() + g.axisDZ() * o;
            boolean opening = Math.abs(o) <= 1;
            for (int depth = -1; depth <= 1; depth++) {
                int dwx = bx + g.inDX() * depth, dwz = bz + g.inDZ() * depth;
                if (opening) {
                    for (int dy = 0; dy < 5; dy++) forceSet(level, dwx, refY + dy, dwz, Blocks.AIR.defaultBlockState());
                    for (int dy = 5; dy < WALL_HEIGHT; dy++) forceSet(level, dwx, refY + dy, dwz, brick); // arch
                    forceSet(level, dwx, refY - 1, dwz, pathBlock);                                       // floor
                    fillDown(level, dwx, refY - 2, dwz, brick);
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
            gradePath(level, bx + oDX, bz + oDZ, oDX, oDZ, refY - 1, 10, false, pathBlock, brick, desert);
            gradePath(level, bx + g.inDX(), bz + g.inDZ(), g.inDX(), g.inDZ(), refY - 1, 40, true, pathBlock, brick, desert);
        }
    }

    /**
     * Lays a graded path from a start cell in direction (dx,dz): the floor ramps toward the
     * natural ground at most 1 block per cell, headroom is cleared above (cutting through
     * rising terrain so a hill can't wall off the gate), and any gap below is filled. If
     * {@code stopAtRoad}, it ends once it reaches an existing village road.
     */
    private static void gradePath(ServerLevel level, int startWX, int startWZ, int dx, int dz,
                                   int startFloorY, int maxLen, boolean stopAtRoad,
                                   BlockState pathBlock, BlockState supportBlock, boolean desert) {
        int floorY = startFloorY;
        for (int t = 1; t <= maxLen; t++) {
            int ix = startWX + dx * t, iz = startWZ + dz * t;
            if (level.getChunkSource().getChunkNow(ix >> 4, iz >> 4) == null) break;
            int natural = groundHeight(level, ix, iz) - 1; // natural surface block Y
            if (stopAtRoad && isVillageRoad(level.getBlockState(new BlockPos(ix, natural, iz)), desert)) break;
            if (natural > floorY) floorY++;
            else if (natural < floorY) floorY--;
            forceSet(level, ix, floorY, iz, pathBlock);                         // path surface
            for (int dy = 1; dy <= 4; dy++) {                                   // cut headroom
                BlockPos p = new BlockPos(ix, floorY + dy, iz);
                BlockState bs = level.getBlockState(p);
                if (!bs.isAir() && !bs.liquid()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            fillDown(level, ix, floorY - 1, iz, supportBlock); // support if floating
        }
    }

    /** Village street surface: DIRT_PATH everywhere; sandstone family in desert villages. */
    private static boolean isVillageRoad(BlockState bs, boolean desert) {
        if (bs.is(Blocks.DIRT_PATH)) return true;
        return desert && (bs.is(Blocks.SMOOTH_SANDSTONE) || bs.is(Blocks.SANDSTONE)
                       || bs.is(Blocks.CUT_SANDSTONE));
    }

    private static boolean isDesertLike(ServerLevel level, int x, int z) {
        return usesSandstoneVillageMaterials(level, x, z);
    }

    private static boolean usesSandstoneVillageMaterials(ServerLevel level, int x, int z) {
        var biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS);
    }

    /** Unconditional block set (overrides existing), chunk-load guarded. */
    private static void forceSet(ServerLevel level, int x, int y, int z, BlockState state) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return;
        level.setBlock(new BlockPos(x, y, z), state, 3);
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
        // Stone-brick family only — no cobblestone.
        var biome = level.getBiome(new BlockPos(x, 64, z));
        if (usesSandstoneVillageMaterials(level, x, z))
            return Blocks.SANDSTONE.defaultBlockState();
        if (biome.is(BiomeTags.IS_FOREST))
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static BlockState brickMaterial(ServerLevel level, int x, int z) {
        if (usesSandstoneVillageMaterials(level, x, z))
            return Blocks.CHISELED_SANDSTONE.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** Battlement / merlon material — biome-adaptive wall-block variant. */
    private static BlockState merlonMaterial(ServerLevel level, int x, int z) {
        // Stone-brick family only — no cobblestone wall.
        if (usesSandstoneVillageMaterials(level, x, z))
            return Blocks.SANDSTONE_WALL.defaultBlockState();
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
        if (!ex.isAir() && !ex.canBeReplaced() && !ex.is(BlockTags.LOGS) && !ex.is(BlockTags.LEAVES)) return;
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
