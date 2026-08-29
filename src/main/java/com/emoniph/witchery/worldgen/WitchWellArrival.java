package com.emoniph.witchery.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Safe exterior arrival and player-scale routes for the three Witch Well variants. */
public final class WitchWellArrival {
    public static final String ID = "witchery:witch_well";

    private static final int SEARCH_RADIUS = 8;
    private static final int NODE_LIMIT = 65_536;

    private WitchWellArrival() {}

    public static boolean isWitchWell(String namespace, String path) {
        return "witchery".equals(namespace)
                && Set.of("witch_well", "small_well", "cursed_well").contains(path);
    }

    public static StructureStart findLoadedStart(ServerLevel level, BlockPos near) {
        return WildStructureArrival.findLoadedStart(level, Identifier.parse(ID), near, 4);
    }

    public static WitchWellPiece findPiece(StructureStart start) {
        if (start == null || !start.isValid()) {
            return null;
        }
        for (var piece : start.getPieces()) {
            if (piece instanceof WitchWellPiece well) {
                return well;
            }
        }
        return null;
    }

    public static BlockPos findLanding(ServerLevel level, StructureStart start) {
        WitchWellPiece piece = findPiece(start);
        if (piece == null) {
            return null;
        }
        BoundingBox box = piece.getBoundingBox();
        WitchWellPiece.Variant variant = piece.variant();
        BlockPos preferred = local(box, variant.surfaceAnchorX(), variant.surfaceYOffset() + 1,
                variant.surfaceMinZ() - 1);
        return WildStructureArrival.findSafeNear(level, preferred, SEARCH_RADIUS, 3, false);
    }

    public static BlockPos traversalTarget(WitchWellPiece piece) {
        BoundingBox box = piece.getBoundingBox();
        return switch (piece.variant()) {
            case REGULAR -> local(box, 8, 2, 4);
            case SMALL -> local(box, 4, 1, 3);
            case CAVE -> local(box, 22, 7, 12);
        };
    }

    public static int traversalDistance(ServerLevel level, BlockPos landing, BlockPos target,
                                        BoundingBox structureBox) {
        return traversal(level, landing, target, structureBox).distance();
    }

    public static TraversalResult traversal(ServerLevel level, BlockPos landing, BlockPos target,
                                            BoundingBox structureBox) {
        if (landing == null || target == null || !canOccupy(level, landing)) {
            return new TraversalResult(-1, 0, 0, landing);
        }
        BoundingBox bounds = new BoundingBox(
                structureBox.minX() - SEARCH_RADIUS, structureBox.minY() - 3,
                structureBox.minZ() - SEARCH_RADIUS, structureBox.maxX() + SEARCH_RADIUS,
                structureBox.maxY() + 4, structureBox.maxZ() + SEARCH_RADIUS);
        ArrayDeque<PathNode> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(new PathNode(landing, 0));
        visited.add(landing.asLong());
        BlockPos nearest = landing;
        double nearestDistance = landing.distSqr(target);

        int inspected = 0;
        while (!queue.isEmpty() && inspected++ < NODE_LIMIT) {
            PathNode node = queue.removeFirst();
            double targetDistance = node.pos().distSqr(target);
            if (targetDistance < nearestDistance) {
                nearest = node.pos();
                nearestDistance = targetDistance;
            }
            if (node.pos().equals(target)) {
                return new TraversalResult(node.distance(), inspected, visited.size(), target);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = node.pos().getX() + direction.getStepX();
                int z = node.pos().getZ() + direction.getStepZ();
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos next = new BlockPos(x, node.pos().getY() + dy, z);
                    if (tryQueue(level, bounds, visited, queue, next, node.distance() + 1)) {
                        break;
                    }
                }
            }
            for (int dy : new int[]{1, -1}) {
                BlockPos next = node.pos().offset(0, dy, 0);
                if (isVerticalRoute(level.getBlockState(node.pos()))
                        || isVerticalRoute(level.getBlockState(next))) {
                    tryQueue(level, bounds, visited, queue, next, node.distance() + 1);
                }
            }
        }
        return new TraversalResult(-1, inspected, visited.size(), nearest);
    }

    public static TraversalResult traversalVia(ServerLevel level, BlockPos landing,
                                               BlockPos waypoint, BlockPos target,
                                               BoundingBox structureBox) {
        return traversalViaSegments(level, landing, waypoint, target, structureBox).combined();
    }

    public static ViaTraversalResult traversalViaSegments(ServerLevel level, BlockPos landing,
                                                           BlockPos waypoint, BlockPos target,
                                                           BoundingBox structureBox) {
        TraversalResult first = traversal(level, landing, waypoint, structureBox);
        if (first.distance() < 0) {
            return new ViaTraversalResult(first, null, first);
        }
        TraversalResult second = traversal(level, waypoint, target, structureBox);
        if (second.distance() < 0) {
            TraversalResult combined = new TraversalResult(
                    -1, first.inspected() + second.inspected(),
                    first.visited() + second.visited(), second.nearest());
            return new ViaTraversalResult(first, second, combined);
        }
        TraversalResult combined = new TraversalResult(
                first.distance() + second.distance(),
                first.inspected() + second.inspected(), first.visited() + second.visited(), target);
        return new ViaTraversalResult(first, second, combined);
    }

    private static boolean tryQueue(ServerLevel level, BoundingBox bounds, Set<Long> visited,
                                    ArrayDeque<PathNode> queue, BlockPos next, int distance) {
        if (!bounds.isInside(next) || !visited.add(next.asLong())) {
            return false;
        }
        if (!canOccupy(level, next)) {
            return false;
        }
        queue.addLast(new PathNode(next, distance));
        return true;
    }

    static boolean canOccupy(ServerLevel level, BlockPos feet) {
        if (WildStructureArrival.canStand(level, feet, true)) {
            return true;
        }
        BlockState state = level.getBlockState(feet);
        if (!isVerticalRoute(state)) {
            return false;
        }
        BlockState head = level.getBlockState(feet.above());
        return head.isAir() || head.canBeReplaced() || isVerticalRoute(head)
                || head.getBlock() instanceof DoorBlock || head.getBlock() instanceof FenceGateBlock;
    }

    private static boolean isVerticalRoute(BlockState state) {
        return state.is(Blocks.LADDER) || !state.getFluidState().isEmpty()
                || state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN);
    }

    private static BlockPos local(BoundingBox box, int x, int y, int z) {
        return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
    }

    private record PathNode(BlockPos pos, int distance) {}

    public record TraversalResult(int distance, int inspected, int visited, BlockPos nearest) {}

    public record ViaTraversalResult(TraversalResult approach, TraversalResult lower,
                                     TraversalResult combined) {}
}
