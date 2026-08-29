package com.emoniph.witchery.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Shared, structure-aware arrival and traversal rules for wild-structure goto and certification. */
public final class WildStructureArrival {

    public static final String COVEN_CIRCLE = "witchery:coven_circle";
    public static final String STRAW_MAN = "witchery:straw_man";
    public static final String HOBGOBLIN_HUT = "witchery:hobgoblin_hut";
    public static final String WITCH_HUT = "witchery:witch_hut";
    public static final String WARPED_COVEN = "witchery:warped_coven";
    public static final String VANISHING_HUT = "witchery:vanishing_hut";

    private static final Set<String> IDS = Set.of(
            COVEN_CIRCLE, STRAW_MAN, HOBGOBLIN_HUT, WITCH_HUT, WARPED_COVEN, VANISHING_HUT);
    private static final int ENTRY_SEARCH_RADIUS = 8;
    private static final int ENTRY_VERTICAL_RANGE = 3;
    private static final int TRAVERSAL_NODE_LIMIT = 16_384;

    private WildStructureArrival() {}

    public static boolean isWildStructure(String namespace, String path) {
        return IDS.contains(namespace + ":" + path);
    }

    public static StructureStart findLoadedStart(ServerLevel level, Identifier id, BlockPos near, int radiusChunks) {
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holder = registry.get(key);
        if (holder.isEmpty()) {
            return null;
        }
        Structure structure = holder.get().value();
        ChunkPos center = new ChunkPos(near);
        StructureStart best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk == null) {
                    continue;
                }
                StructureStart start = chunk.getAllStarts().get(structure);
                if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
                    continue;
                }
                BoundingBox box = start.getBoundingBox();
                if (containsXZ(box, near)) {
                    return start;
                }
                long distance = distanceToCenterSqr(box, near);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = start;
                }
            }
        }
        return best;
    }

    public static BlockPos findLanding(ServerLevel level, Identifier id, StructureStart start) {
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            return null;
        }
        BlockPos preferred = preferredLanding(id.toString(), start.getPieces().getFirst().getBoundingBox());
        return findSafeNear(level, preferred, ENTRY_SEARCH_RADIUS, ENTRY_VERTICAL_RANGE,
                WARPED_COVEN.equals(id.toString()));
    }

    public static BlockPos findVanishingHutLanding(ServerLevel level, BlockPos anchor) {
        return findSafeNear(level, VanishingHut.arrivalPos(anchor), 4, 2, false);
    }

    public static BlockPos traversalTarget(String id, BoundingBox box) {
        return switch (id) {
            case COVEN_CIRCLE -> local(box, 5, 1, 3);
            case STRAW_MAN -> local(box, 2, 1, 1);
            case HOBGOBLIN_HUT -> local(box, 6, 3, 9);
            case WITCH_HUT -> local(box, 1, 1, 2);
            case WARPED_COVEN -> local(box, 7, 2, 7);
            case VANISHING_HUT -> {
                BlockPos anchor = vanishingAnchor(box);
                yield VanishingHut.residentHome(anchor);
            }
            default -> null;
        };
    }

    public static int traversalDistance(ServerLevel level, BlockPos landing, BlockPos target,
                                        BoundingBox structureBox) {
        if (landing == null || target == null || !canStand(level, landing, false)) {
            return -1;
        }
        BoundingBox bounds = new BoundingBox(
                structureBox.minX() - ENTRY_SEARCH_RADIUS,
                structureBox.minY() - 3,
                structureBox.minZ() - ENTRY_SEARCH_RADIUS,
                structureBox.maxX() + ENTRY_SEARCH_RADIUS,
                structureBox.maxY() + 5,
                structureBox.maxZ() + ENTRY_SEARCH_RADIUS);
        ArrayDeque<PathNode> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(new PathNode(landing, 0));
        visited.add(landing.asLong());

        int inspected = 0;
        while (!queue.isEmpty() && inspected++ < TRAVERSAL_NODE_LIMIT) {
            PathNode node = queue.removeFirst();
            if (node.pos().equals(target)) {
                return node.distance();
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = node.pos().getX() + direction.getStepX();
                int z = node.pos().getZ() + direction.getStepZ();
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos next = new BlockPos(x, node.pos().getY() + dy, z);
                    if (!bounds.isInside(next) || !visited.add(next.asLong())) {
                        continue;
                    }
                    if (canStand(level, next, true)) {
                        queue.addLast(new PathNode(next, node.distance() + 1));
                        break;
                    }
                }
            }
        }
        return -1;
    }

    public static boolean isSafeLanding(ServerLevel level, BlockPos pos) {
        return canStand(level, pos, false) && !hasImmediateHazard(level, pos);
    }

    private static BlockPos preferredLanding(String id, BoundingBox box) {
        return switch (id) {
            case COVEN_CIRCLE -> local(box, 5, 1, -1);
            case STRAW_MAN -> local(box, 2, 1, -1);
            case HOBGOBLIN_HUT -> local(box, 6, 3, 11);
            case WITCH_HUT -> local(box, 1, 1, -1);
            case WARPED_COVEN -> local(box, 7, 2, 5);
            case VANISHING_HUT -> VanishingHut.arrivalPos(vanishingAnchor(box));
            default -> new BlockPos((box.minX() + box.maxX()) / 2, box.minY() + 1,
                    (box.minZ() + box.maxZ()) / 2);
        };
    }

    static BlockPos findSafeNear(ServerLevel level, BlockPos preferred, int radius,
                                 int verticalRange, boolean nether) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    int x = preferred.getX() + dx;
                    int z = preferred.getZ() + dz;
                    if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
                        continue;
                    }
                    for (int offset = 0; offset <= verticalRange; offset++) {
                        BlockPos up = new BlockPos(x, preferred.getY() + offset, z);
                        if (canUseLanding(level, up, nether)) {
                            return up;
                        }
                        if (offset > 0) {
                            BlockPos down = new BlockPos(x, preferred.getY() - offset, z);
                            if (canUseLanding(level, down, nether)) {
                                return down;
                            }
                        }
                    }
                }
            }
        }

        // Final ground-level fallback remains near the intended entrance, never on the roof centre.
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    int x = preferred.getX() + dx;
                    int z = preferred.getZ() + dz;
                    if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
                        continue;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos surface = new BlockPos(x, y, z);
                    if (canUseLanding(level, surface, nether)) {
                        return surface;
                    }
                }
            }
        }
        return null;
    }

    private static boolean canUseLanding(ServerLevel level, BlockPos pos, boolean nether) {
        return canStand(level, pos, false) && (!nether || !hasImmediateHazard(level, pos));
    }

    static boolean canStand(ServerLevel level, BlockPos feet, boolean allowOpenableBlocks) {
        if (feet.getY() <= level.getMinY() || feet.getY() >= level.getMaxY() - 1) {
            return false;
        }
        BlockPos groundPos = feet.below();
        BlockState ground = level.getBlockState(groundPos);
        return ground.getFluidState().isEmpty()
                && isWalkableSupport(level, groundPos, ground)
                && isOpen(level, feet, level.getBlockState(feet), allowOpenableBlocks)
                && isOpen(level, feet.above(), level.getBlockState(feet.above()),
                allowOpenableBlocks);
    }

    private static boolean isWalkableSupport(ServerLevel level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) <= 1.0D;
    }

    private static boolean isOpen(ServerLevel level, BlockPos pos, BlockState state,
                                  boolean allowOpenableBlocks) {
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.isAir() || state.canBeReplaced()) {
            return true;
        }
        if (allowOpenableBlocks
                && (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock)) {
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean hasImmediateHazard(ServerLevel level, BlockPos feet) {
        BlockState floor = level.getBlockState(feet.below());
        if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CAMPFIRE)
                || floor.is(Blocks.SOUL_CAMPFIRE)) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                            || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BlockPos local(BoundingBox box, int x, int y, int z) {
        return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
    }

    private static BlockPos vanishingAnchor(BoundingBox box) {
        return new BlockPos(box.minX() - VanishingHut.MIN_DX, box.minY(),
                box.minZ() - VanishingHut.MIN_DZ);
    }

    private static boolean containsXZ(BoundingBox box, BlockPos pos) {
        return pos.getX() >= box.minX() && pos.getX() <= box.maxX()
                && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ();
    }

    private static long distanceToCenterSqr(BoundingBox box, BlockPos pos) {
        long dx = pos.getX() - (long) (box.minX() + box.maxX()) / 2L;
        long dz = pos.getZ() - (long) (box.minZ() + box.maxZ()) / 2L;
        return dx * dx + dz * dz;
    }

    private record PathNode(BlockPos pos, int distance) {}
}
