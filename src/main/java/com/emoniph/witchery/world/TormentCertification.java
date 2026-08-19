package com.emoniph.witchery.world;

import com.emoniph.witchery.blocks.TormentPortalBlock;
import com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Full loaded-world certificate for the deterministic Torment maze footprint. */
public final class TormentCertification {
    private static final int MAX_FAILURE_DETAILS = 96;
    private static final int[] HORIZONTAL_X = {0, 0, 1, -1};
    private static final int[] HORIZONTAL_Z = {-1, 1, 0, 0};

    private TormentCertification() {}

    public static Report audit(ServerLevel torment) {
        loadFootprintChunks(torment);
        FailureCollector failures = new FailureCollector();
        TormentMazeLayout layout = TormentMazeLayout.forSeed(torment.getSeed());
        List<LevelReport> levels = new ArrayList<>(TormentHelper.NUM_LEVELS);
        for (int level = 0; level < TormentHelper.NUM_LEVELS; level++) {
            levels.add(auditLevel(torment, layout.level(level), failures));
        }
        return new Report(torment.getSeed(), List.copyOf(levels), failures.total(), failures.details());
    }

    public static void loadFootprintChunks(ServerLevel torment) {
        int minChunkX = Math.floorDiv(TormentHelper.STRUCTURE_MIN_X, 16);
        int maxChunkX = Math.floorDiv(TormentHelper.STRUCTURE_MAX_X, 16);
        int minChunkZ = Math.floorDiv(TormentHelper.STRUCTURE_MIN_Z, 16);
        int maxChunkZ = Math.floorDiv(TormentHelper.STRUCTURE_MAX_Z, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                torment.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static LevelReport auditLevel(ServerLevel world, TormentMazeLayout.MazeLevel maze,
                                          FailureCollector failures) {
        int level = maze.level();
        int mazeY = TormentHelper.getMazeY(level);
        Set<BlockPos> chestPositions = maze.chestPositions();
        int passageColumns = 0;
        int wallColumns = 0;
        int checks = 0;

        for (int x = TormentHelper.STRUCTURE_MIN_X; x <= TormentHelper.STRUCTURE_MAX_X; x++) {
            for (int z = TormentHelper.STRUCTURE_MIN_Z; z <= TormentHelper.STRUCTURE_MAX_Z; z++) {
                TormentMazeLayout.PlanarKind kind = maze.kindAt(x, z);
                if (kind == TormentMazeLayout.PlanarKind.OUTSIDE) continue;
                if (kind == TormentMazeLayout.PlanarKind.WALL) {
                    wallColumns++;
                    checks += certifyWallColumn(world, level, mazeY, x, z, failures);
                } else {
                    passageColumns++;
                    checks += certifyPassageColumn(world, level, mazeY, x, z,
                            chestPositions.contains(new BlockPos(x, mazeY, z)), failures);
                }
            }
        }

        int foundChests = 0;
        int filledChests = 0;
        for (int x = TormentHelper.STRUCTURE_MIN_X; x <= TormentHelper.STRUCTURE_MAX_X; x++) {
            for (int z = TormentHelper.STRUCTURE_MIN_Z; z <= TormentHelper.STRUCTURE_MAX_Z; z++) {
                BlockPos pos = new BlockPos(x, mazeY, z);
                if (!world.getBlockState(pos).is(WitcheryBlocks.REFILLING_CHEST.get())) continue;
                foundChests++;
                if (!(world.getBlockEntity(pos) instanceof RefillingChestBlockEntity chest)) {
                    failures.add("Level " + level + " Refilling Chest has no block entity at " + pos + ".");
                } else if (!chest.isEmpty()) {
                    filledChests++;
                }
            }
        }
        if (foundChests != chestPositions.size()) {
            failures.add("Level " + level + " expected exactly " + chestPositions.size()
                    + " Refilling Chests but found " + foundChests + ".");
        }

        boolean route = hasActualEntryToPortalRoute(world, level);
        if (!route) {
            failures.add("Level " + level + " has no walkable entry-to-portal route.");
        }
        if (!maze.allCellsConnected() || maze.cellEdgeCount() != TormentHelper.MAZE_SIZE * TormentHelper.MAZE_SIZE - 1) {
            failures.add("Level " + level + " deterministic DFS plan is not a perfect maze.");
        }
        if (!maze.hasEntryToPortalRoute()) {
            failures.add("Level " + level + " deterministic plan does not connect entry and portal chambers.");
        }

        return new LevelReport(level, maze.fingerprint(), passageColumns, wallColumns,
                checks, foundChests, filledChests, route);
    }

    private static int certifyWallColumn(ServerLevel world, int level, int mazeY, int x, int z,
                                         FailureCollector failures) {
        int checks = 0;
        for (int y = mazeY; y <= mazeY + 5; y++) {
            checks++;
            expect(world, new BlockPos(x, y, z), state -> state.is(WitcheryBlocks.FORCE.get()),
                    "Level " + level + " wall", failures);
        }
        if (x == TormentHelper.ORIGIN_X && z == TormentHelper.ORIGIN_Z) {
            BlockState force = world.getBlockState(new BlockPos(x, mazeY, z));
            if (force.getRenderShape() != RenderShape.INVISIBLE
                    || force.getLightBlock() != 0
                    || !force.propagatesSkylightDown()) {
                failures.add("Level " + level + " Force walls lost invisible, light-transparent source behavior.");
            }
        }
        return checks;
    }

    private static int certifyPassageColumn(ServerLevel world, int level, int mazeY, int x, int z,
                                            boolean chest, FailureCollector failures) {
        int checks = 0;
        BlockPos foundation = new BlockPos(x, mazeY - 1, z);
        checks++;
        expect(world, foundation, state -> state.is(WitcheryBlocks.TORMENT_STONE.get()),
                "Level " + level + " passage foundation", failures);

        BlockPos floor = foundation.above();
        checks++;
        if (chest) {
            expect(world, floor, state -> state.is(WitcheryBlocks.REFILLING_CHEST.get()),
                    "Level " + level + " chest", failures);
        } else {
            expect(world, floor, state -> state.is(WitcheryBlocks.TORMENT_STONE.get()) || state.is(Blocks.MYCELIUM),
                    "Level " + level + " passage floor", failures);
        }
        if (x == TormentHelper.ENTRY_X && z == TormentHelper.ENTRY_Z) {
            BlockState tormentStone = world.getBlockState(foundation);
            if (tormentStone.getRenderShape() != RenderShape.MODEL
                    || tormentStone.getLightBlock() != 0
                    || !tormentStone.propagatesSkylightDown()) {
                failures.add("Level " + level
                        + " Torment Stone lost visible, light-transparent source behavior.");
            }
        }

        for (int offset = 1; offset <= 4; offset++) {
            BlockPos pos = new BlockPos(x, mazeY + offset, z);
            checks++;
            Predicate<BlockState> expected = portalFrameExpectation(x, z, offset);
            expect(world, pos, expected == null ? BlockState::isAir : expected,
                    "Level " + level + " passage interior", failures);
        }

        checks++;
        expect(world, new BlockPos(x, mazeY + 5, z), state -> state.is(WitcheryBlocks.FORCE.get()),
                "Level " + level + " passage ceiling", failures);
        return checks;
    }

    private static Predicate<BlockState> portalFrameExpectation(int x, int z, int offset) {
        if (z != TormentHelper.PORTAL_Z) return null;
        if (x == TormentHelper.PORTAL_X && (offset == 1 || offset == 2)) {
            return state -> state.is(WitcheryBlocks.TORMENT_PORTAL.get())
                    && state.hasProperty(TormentPortalBlock.AXIS)
                    && state.getValue(TormentPortalBlock.AXIS) == Direction.Axis.X;
        }
        if (x == TormentHelper.PORTAL_X && offset == 3) {
            return state -> state.is(WitcheryBlocks.TORMENT_STONE.get());
        }
        if (Math.abs(x - TormentHelper.PORTAL_X) == 1 && offset <= 3) {
            return state -> state.is(WitcheryBlocks.TORMENT_STONE.get());
        }
        return null;
    }

    private static boolean hasActualEntryToPortalRoute(ServerLevel world, int level) {
        int mazeY = TormentHelper.getMazeY(level);
        Set<Long> visited = new HashSet<>();
        ArrayDeque<PlanarPoint> queue = new ArrayDeque<>();
        for (int x = TormentHelper.ENTRY_X - 1; x <= TormentHelper.ENTRY_X + 1; x++) {
            for (int z = TormentHelper.ENTRY_Z - 1; z <= TormentHelper.ENTRY_Z + 1; z++) {
                if (walkable(world, mazeY, x, z) && visited.add(pack(x, z))) {
                    queue.addLast(new PlanarPoint(x, z));
                }
            }
        }

        int targetZ = TormentHelper.PORTAL_Z - 1;
        while (!queue.isEmpty()) {
            PlanarPoint point = queue.removeFirst();
            if (point.x == TormentHelper.PORTAL_X && point.z == targetZ) return true;
            for (int direction = 0; direction < HORIZONTAL_X.length; direction++) {
                int nextX = point.x + HORIZONTAL_X[direction];
                int nextZ = point.z + HORIZONTAL_Z[direction];
                if (nextX < TormentHelper.STRUCTURE_MIN_X || nextX > TormentHelper.STRUCTURE_MAX_X
                        || nextZ < TormentHelper.STRUCTURE_MIN_Z || nextZ > TormentHelper.STRUCTURE_MAX_Z
                        || !visited.add(pack(nextX, nextZ))) {
                    continue;
                }
                if (walkable(world, mazeY, nextX, nextZ)) {
                    queue.addLast(new PlanarPoint(nextX, nextZ));
                }
            }
        }
        return false;
    }

    private static boolean walkable(ServerLevel world, int mazeY, int x, int z) {
        BlockPos floor = new BlockPos(x, mazeY, z);
        return !world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()
                && world.getBlockState(floor.above()).getCollisionShape(world, floor.above()).isEmpty()
                && world.getBlockState(floor.above(2)).getCollisionShape(world, floor.above(2)).isEmpty();
    }

    private static void expect(ServerLevel world, BlockPos pos, Predicate<BlockState> predicate,
                               String label, FailureCollector failures) {
        BlockState found = world.getBlockState(pos);
        if (!predicate.test(found)) {
            failures.add(label + " mismatch at " + pos + ": found "
                    + BuiltInRegistries.BLOCK.getKey(found.getBlock()) + ".");
        }
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public record Report(long seed, List<LevelReport> levels, int mismatchCount, List<String> failures) {
        public boolean passed() {
            return mismatchCount == 0;
        }

        public int filledChestCount() {
            return levels.stream().mapToInt(LevelReport::filledChests).sum();
        }

        public int chestCount() {
            return levels.stream().mapToInt(LevelReport::foundChests).sum();
        }
    }

    public record LevelReport(int level, String fingerprint, int passageColumns, int wallColumns,
                              int checkedBlocks, int foundChests, int filledChests,
                              boolean entryToPortalRoute) {}

    private static final class FailureCollector {
        private final List<String> details = new ArrayList<>();
        private int total;

        void add(String failure) {
            total++;
            if (details.size() < MAX_FAILURE_DETAILS) details.add(failure);
        }

        int total() {
            return total;
        }

        List<String> details() {
            if (total > details.size()) {
                details.add("... " + (total - details.size()) + " additional mismatch(es) omitted.");
            }
            return List.copyOf(details);
        }
    }

    private record PlanarPoint(int x, int z) {}
}
