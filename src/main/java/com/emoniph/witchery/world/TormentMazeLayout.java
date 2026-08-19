package com.emoniph.witchery.world;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic, world-independent description of the six source-shaped Torment mazes. */
public final class TormentMazeLayout {
    static final int NORTH = 1;
    static final int SOUTH = 2;
    static final int EAST = 4;
    static final int WEST = 8;

    private static final int[] DX = {0, 0, 1, -1};
    private static final int[] DZ = {-1, 1, 0, 0};
    private static final int[] BITS = {NORTH, SOUTH, EAST, WEST};
    private static final int[] OPPOSITE = {SOUTH, NORTH, WEST, EAST};
    private static final ConcurrentHashMap<Long, TormentMazeLayout> CACHE = new ConcurrentHashMap<>();

    private final MazeLevel[] levels;

    private TormentMazeLayout(long seed) {
        levels = new MazeLevel[TormentHelper.NUM_LEVELS];
        for (int level = 0; level < levels.length; level++) {
            int[][] cells = new int[TormentHelper.MAZE_SIZE][TormentHelper.MAZE_SIZE];
            boolean[][] visited = new boolean[TormentHelper.MAZE_SIZE][TormentHelper.MAZE_SIZE];
            Random random = new Random(seed ^ ((long) level * 0x9e3779b97f4a7c15L));
            carve(0, 0, cells, visited, random);
            levels[level] = new MazeLevel(level, cells,
                    chestShift(seed, level, 0), chestShift(seed, level, 1));
        }
    }

    public static TormentMazeLayout forSeed(long seed) {
        return CACHE.computeIfAbsent(seed, TormentMazeLayout::new);
    }

    public MazeLevel level(int level) {
        return levels[Math.clamp(level, 0, levels.length - 1)];
    }

    private static void carve(int cellX, int cellZ, int[][] cells, boolean[][] visited, Random random) {
        visited[cellX][cellZ] = true;
        Integer[] directions = {0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(directions), random);
        for (int direction : directions) {
            int nextX = cellX + DX[direction];
            int nextZ = cellZ + DZ[direction];
            if (nextX < 0 || nextX >= TormentHelper.MAZE_SIZE
                    || nextZ < 0 || nextZ >= TormentHelper.MAZE_SIZE
                    || visited[nextX][nextZ]) {
                continue;
            }
            cells[cellX][cellZ] |= BITS[direction];
            cells[nextX][nextZ] |= OPPOSITE[direction];
            carve(nextX, nextZ, cells, visited, random);
        }
    }

    private static int chestShift(long seed, int level, int index) {
        long hash = seed ^ ((long) level * 0x632be59bd9b4e019L) ^ ((long) index * 0x85157af5L);
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        return (int) Math.floorMod(hash, 11L) - 5;
    }

    public enum PlanarKind {
        OUTSIDE,
        WALL,
        PASSAGE
    }

    public static final class MazeLevel {
        private final int level;
        private final int[][] cells;
        private final int leftChestShift;
        private final int rightChestShift;
        private final String fingerprint;

        private MazeLevel(int level, int[][] cells, int leftChestShift, int rightChestShift) {
            this.level = level;
            this.cells = cells;
            this.leftChestShift = leftChestShift;
            this.rightChestShift = rightChestShift;
            this.fingerprint = fingerprint(cells, leftChestShift, rightChestShift);
        }

        int connectionMask(int cellX, int cellZ) {
            return cells[cellX][cellZ];
        }

        public int level() {
            return level;
        }

        public int leftChestShift() {
            return leftChestShift;
        }

        public int rightChestShift() {
            return rightChestShift;
        }

        public String fingerprint() {
            return fingerprint;
        }

        public Set<BlockPos> chestPositions() {
            int y = TormentHelper.getMazeY(level);
            return Set.of(
                    new BlockPos(TormentHelper.ORIGIN_X + 3, y,
                            TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + leftChestShift),
                    new BlockPos(TormentHelper.ORIGIN_X + TormentHelper.MAZE_SIZE, y,
                            TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE),
                    new BlockPos(TormentHelper.ORIGIN_X + TormentHelper.MAZE_SIZE * 2 - 3, y,
                            TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + rightChestShift));
        }

        public PlanarKind kindAt(int worldX, int worldZ) {
            if (inCarvedRoom(worldX, worldZ)) {
                return PlanarKind.PASSAGE;
            }

            int localX = worldX - TormentHelper.ORIGIN_X;
            int localZ = worldZ - TormentHelper.ORIGIN_Z;
            int mazeSpan = TormentHelper.MAZE_SIZE * 2;
            if (localX < 0 || localX > mazeSpan || localZ < 0 || localZ > mazeSpan) {
                return PlanarKind.OUTSIDE;
            }

            if ((localZ & 1) == 0) {
                if ((localX & 1) == 0 || localZ == mazeSpan) {
                    return PlanarKind.WALL;
                }
                int cellX = (localX - 1) / 2;
                int cellZ = localZ / 2;
                return (cells[cellX][cellZ] & NORTH) != 0
                        ? PlanarKind.PASSAGE : PlanarKind.WALL;
            }

            if ((localX & 1) == 1) {
                return PlanarKind.PASSAGE;
            }
            if (localX == mazeSpan) {
                return PlanarKind.WALL;
            }
            int cellX = localX / 2;
            int cellZ = (localZ - 1) / 2;
            return (cells[cellX][cellZ] & WEST) != 0
                    ? PlanarKind.PASSAGE : PlanarKind.WALL;
        }

        public int cellEdgeCount() {
            int edges = 0;
            for (int x = 0; x < cells.length; x++) {
                for (int z = 0; z < cells[x].length; z++) {
                    if ((cells[x][z] & EAST) != 0) edges++;
                    if ((cells[x][z] & SOUTH) != 0) edges++;
                }
            }
            return edges;
        }

        public boolean allCellsConnected() {
            boolean[][] visited = new boolean[TormentHelper.MAZE_SIZE][TormentHelper.MAZE_SIZE];
            ArrayDeque<Cell> queue = new ArrayDeque<>();
            queue.add(new Cell(0, 0));
            visited[0][0] = true;
            int count = 0;
            while (!queue.isEmpty()) {
                Cell cell = queue.removeFirst();
                count++;
                int mask = cells[cell.x][cell.z];
                for (int direction = 0; direction < BITS.length; direction++) {
                    if ((mask & BITS[direction]) == 0) continue;
                    int nextX = cell.x + DX[direction];
                    int nextZ = cell.z + DZ[direction];
                    if (!visited[nextX][nextZ]) {
                        visited[nextX][nextZ] = true;
                        queue.addLast(new Cell(nextX, nextZ));
                    }
                }
            }
            return count == TormentHelper.MAZE_SIZE * TormentHelper.MAZE_SIZE;
        }

        public boolean hasEntryToPortalRoute() {
            Set<Long> visited = new HashSet<>();
            ArrayDeque<PlanarPoint> queue = new ArrayDeque<>();
            for (int x = TormentHelper.ENTRY_X - 1; x <= TormentHelper.ENTRY_X + 1; x++) {
                for (int z = TormentHelper.ENTRY_Z - 1; z <= TormentHelper.ENTRY_Z + 1; z++) {
                    if (kindAt(x, z) == PlanarKind.PASSAGE && visited.add(pack(x, z))) {
                        queue.addLast(new PlanarPoint(x, z));
                    }
                }
            }

            int targetX = TormentHelper.PORTAL_X;
            int targetZ = TormentHelper.PORTAL_Z - 1;
            while (!queue.isEmpty()) {
                PlanarPoint point = queue.removeFirst();
                if (point.x == targetX && point.z == targetZ) {
                    return true;
                }
                for (int direction = 0; direction < DX.length; direction++) {
                    int nextX = point.x + DX[direction];
                    int nextZ = point.z + DZ[direction];
                    if (kindAt(nextX, nextZ) == PlanarKind.PASSAGE && visited.add(pack(nextX, nextZ))) {
                        queue.addLast(new PlanarPoint(nextX, nextZ));
                    }
                }
            }
            return false;
        }

        private boolean inCarvedRoom(int x, int z) {
            int centerX = TormentHelper.PORTAL_X;
            int leftRoomCenterZ = TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + leftChestShift;
            int rightRoomCenterZ = TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + rightChestShift;
            return inRectangle(x, z, centerX - 3, centerX + 3,
                    TormentHelper.ORIGIN_Z + 1, TormentHelper.ORIGIN_Z + 7)
                    || inRectangle(x, z, centerX - 3, centerX + 3,
                    TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE * 2 - 7,
                    TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE * 2 + 1)
                    || inRectangle(x, z, TormentHelper.ORIGIN_X + 1, TormentHelper.ORIGIN_X + 5,
                    leftRoomCenterZ - 2, leftRoomCenterZ + 2)
                    || inRectangle(x, z,
                    TormentHelper.ORIGIN_X + TormentHelper.MAZE_SIZE * 2 - 5,
                    TormentHelper.ORIGIN_X + TormentHelper.MAZE_SIZE * 2 - 1,
                    rightRoomCenterZ - 2, rightRoomCenterZ + 2)
                    || inRectangle(x, z, centerX - 3, centerX + 3,
                    TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE - 3,
                    TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + 3);
        }

        private static boolean inRectangle(int x, int z, int minX, int maxX, int minZ, int maxZ) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        private static String fingerprint(int[][] cells, int leftShift, int rightShift) {
            long hash = 0xcbf29ce484222325L;
            for (int[] column : cells) {
                for (int mask : column) {
                    hash ^= mask;
                    hash *= 0x100000001b3L;
                }
            }
            hash ^= leftShift + 5;
            hash *= 0x100000001b3L;
            hash ^= rightShift + 5;
            hash *= 0x100000001b3L;
            return Long.toUnsignedString(hash, 16);
        }

        private static long pack(int x, int z) {
            return ((long) x << 32) ^ (z & 0xffffffffL);
        }
    }

    private record Cell(int x, int z) {}

    private record PlanarPoint(int x, int z) {}
}
