package com.emoniph.witchery.world;

import com.emoniph.witchery.blocks.MirrorBlock;
import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.MirrorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.Set;

/** Server-authoritative Mirror World room, guardian, and return-route lifecycle. */
public final class MirrorHelper {
    private MirrorHelper() {}

    public static boolean enterMirrorWorld(ServerPlayer player, ServerLevel sourceLevel,
                                           BlockPos mirrorPos) {
        return enterRoom(player, sourceLevel, mirrorPos);
    }

    private static boolean enterRoom(ServerPlayer player, ServerLevel sourceLevel, BlockPos mirrorPos) {
        BlockPos sourceFoot = mirrorFoot(sourceLevel, mirrorPos);
        MirrorBlockEntity sourceMirror = getMirrorBlockEntity(sourceLevel, sourceFoot);
        if (sourceMirror == null || sourceLevel.getBlockState(sourceFoot).getValue(MirrorBlock.UNBREAKABLE)) {
            return false;
        }

        MinecraftServer server = sourceLevel.getServer();
        ServerLevel mirrorLevel = server.getLevel(WitcheryDimensions.MIRROR_WORLD);
        if (mirrorLevel == null) {
            return false;
        }

        MirrorRoomData roomData = MirrorRoomData.get(server);
        MirrorRoomData.Room room = resolveRoom(roomData, sourceLevel, sourceFoot);
        sourceMirror.setRoom(room.chunkX(), room.sectionY(), room.chunkZ());
        mirrorLevel.getChunk(room.chunkX(), room.chunkZ());
        placeInnerMirror(mirrorLevel, room, sourceMirror.isDemonKilled());

        BlockPos entry = findRoomEntry(mirrorLevel, room.chunkX(), room.sectionY(), room.chunkZ());
        WitcheryPlayerData playerData = WitcheryAttachments.get(player);
        int oldDepth = playerData.getMirrorRouteDepth();
        playerData.pushMirrorRoute(sourceFoot, sourceLevel.dimension(), room.index());

        if (!teleport(player, mirrorLevel, entry, 180.0F, 0.0F)) {
            if (playerData.getMirrorRouteDepth() > oldDepth) {
                playerData.popMirrorRoute();
            }
            return false;
        }

        if (!sourceMirror.isDemonKilled()) {
            spawnRoomGuardian(mirrorLevel, room, player);
        }
        return true;
    }

    private static MirrorRoomData.Room resolveRoom(MirrorRoomData roomData, ServerLevel sourceLevel,
                                                    BlockPos sourceFoot) {
        String dimension = sourceLevel.dimension().identifier().toString();
        Optional<MirrorRoomData.Room> owned = roomData.roomForSource(dimension, sourceFoot);
        if (owned.isPresent()) {
            return owned.get();
        }

        // Fresh-world rules are strict: an unrecorded or conflicting BE assignment is replaced.
        return roomData.getOrAllocate(sourceLevel.dimension(), sourceFoot);
    }

    /** Return through an unbreakable room mirror, using the route stack as a persisted fallback. */
    public static boolean exitMirrorWorld(ServerPlayer player, ServerLevel mirrorLevel,
                                          BlockPos innerMirrorPos) {
        if (!player.level().dimension().equals(WitcheryDimensions.MIRROR_WORLD)) {
            return false;
        }

        BlockPos innerFoot = mirrorFoot(mirrorLevel, innerMirrorPos);
        MirrorBlockEntity innerMirror = getMirrorBlockEntity(mirrorLevel, innerFoot);
        if (innerMirror == null
                || !mirrorLevel.getBlockState(innerFoot).getValue(MirrorBlock.UNBREAKABLE)) {
            return false;
        }

        int currentRoomIndex = MirrorWorldLayout.indexForRoom(
                Math.floorDiv(innerFoot.getX(), 16), MirrorWorldLayout.sectionY(innerFoot.getY()),
                Math.floorDiv(innerFoot.getZ(), 16));
        WitcheryPlayerData playerData = WitcheryAttachments.get(player);
        Optional<WitcheryPlayerData.MirrorRoute> route = playerData.getCurrentMirrorRoute()
                .filter(value -> value.destinationRoomIndex() == currentRoomIndex);

        String destinationDimension = innerMirror.getLinkedDim();
        BlockPos destinationMirror = innerMirror.getLinkedPos();
        ServerLevel linkedLevel = destinationDimension == null ? null
                : resolveLevelByKey(mirrorLevel.getServer(), destinationDimension);
        boolean linkedMirrorPresent = linkedLevel != null && destinationMirror != null
                && getMirrorBlockEntity(linkedLevel, destinationMirror) != null;
        if (!innerMirror.isConnected() || !linkedMirrorPresent) {
            Optional<MirrorCarrier> carrier = findCarriedMirror(
                    mirrorLevel.getServer(), currentRoomIndex, player);
            if (carrier.isPresent()) {
                ServerLevel carrierLevel = (ServerLevel) carrier.get().player().level();
                BlockPos landing = findSafeLanding(carrierLevel,
                        carrier.get().player().blockPosition(), 3);
                if (landing == null) landing = carrier.get().player().blockPosition();
                if (teleport(player, carrierLevel, landing,
                        player.getYRot(), player.getXRot())) {
                    if (route.isPresent()) playerData.popMirrorRoute();
                    return true;
                }
            }
        }
        if ((destinationDimension == null || destinationMirror == null) && route.isPresent()) {
            destinationDimension = route.get().sourceDimension();
            destinationMirror = route.get().sourceBlockPos();
        }

        ServerLevel destination = destinationDimension == null
                ? null : resolveLevelByKey(mirrorLevel.getServer(), destinationDimension);
        if (destination == null || destinationMirror == null) {
            return emergencyReturn(player, mirrorLevel.getServer(), playerData);
        }

        BlockPos landing = findLandingBesideMirror(destination, destinationMirror);
        if (landing == null || !teleport(player, destination, landing, player.getYRot(), player.getXRot())) {
            return false;
        }

        if (route.isPresent()) {
            playerData.popMirrorRoute();
        }
        return true;
    }

    private static boolean emergencyReturn(ServerPlayer player, MinecraftServer server,
                                           WitcheryPlayerData playerData) {
        Optional<WitcheryPlayerData.MirrorRoute> route = playerData.getCurrentMirrorRoute();
        if (route.isPresent()) {
            ServerLevel routedLevel = resolveLevelByKey(server, route.get().sourceDimension());
            if (routedLevel != null) {
                BlockPos landing = findLandingBesideMirror(routedLevel, route.get().sourceBlockPos());
                if (landing != null && teleport(player, routedLevel, landing,
                        player.getYRot(), player.getXRot())) {
                    playerData.popMirrorRoute();
                    return true;
                }
            }
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return false;
        }
        BlockPos landing = findSafeLanding(overworld, overworld.getLevelData().getRespawnData().pos(), 8);
        if (landing == null || !teleport(player, overworld, landing, player.getYRot(), player.getXRot())) {
            return false;
        }
        playerData.clearMirrorRoutes();
        return true;
    }

    private static void placeInnerMirror(ServerLevel mirrorLevel, MirrorRoomData.Room room,
                                         boolean cleared) {
        BlockPos bottomPos = MirrorWorldLayout.innerMirrorFoot(room.chunkX(), room.sectionY(), room.chunkZ());
        BlockState bottomState = WitcheryBlocks.MIRROR.get().defaultBlockState()
                .setValue(MirrorBlock.FACING, Direction.EAST)
                .setValue(MirrorBlock.PART, BedPart.FOOT)
                .setValue(MirrorBlock.UNBREAKABLE, true);
        mirrorLevel.setBlockAndUpdate(bottomPos, bottomState);
        mirrorLevel.setBlockAndUpdate(bottomPos.above(), bottomState.setValue(MirrorBlock.PART, BedPart.HEAD));

        MirrorBlockEntity innerMirror = getMirrorBlockEntity(mirrorLevel, bottomPos);
        if (innerMirror != null) {
            innerMirror.setLinkedPos(room.sourceBlockPos());
            innerMirror.setLinkedDim(room.sourceDimension());
            innerMirror.setDemonKilled(cleared);
            innerMirror.setConnected(true);
        }
    }

    public static void disconnectSourceMirror(ServerLevel sourceLevel, MirrorBlockEntity sourceMirror) {
        if (!sourceMirror.hasRoom()) return;
        int[] room = sourceMirror.getRoom();
        int roomIndex = MirrorWorldLayout.indexForRoom(room[0], room[1], room[2]);
        if (roomIndex < 0) return;
        ServerLevel mirrorLevel = sourceLevel.getServer().getLevel(WitcheryDimensions.MIRROR_WORLD);
        if (mirrorLevel == null) return;
        MirrorBlockEntity inner = getMirrorBlockEntity(mirrorLevel,
                MirrorWorldLayout.innerMirrorFoot(room[0], room[1], room[2]));
        if (inner != null) inner.setConnected(false);
    }

    public static void reconnectSourceMirror(ServerLevel sourceLevel, BlockPos sourcePos,
                                             MirrorBlockEntity sourceMirror) {
        if (!sourceMirror.hasRoom()) return;
        int[] coordinates = sourceMirror.getRoom();
        int roomIndex = MirrorWorldLayout.indexForRoom(
                coordinates[0], coordinates[1], coordinates[2]);
        if (roomIndex < 0) return;
        MirrorRoomData data = MirrorRoomData.get(sourceLevel.getServer());
        if (!data.rebindRoom(roomIndex, sourceLevel.dimension(), sourcePos)) return;
        ServerLevel mirrorLevel = sourceLevel.getServer().getLevel(WitcheryDimensions.MIRROR_WORLD);
        if (mirrorLevel == null) return;
        data.roomAtIndex(roomIndex).ifPresent(room -> {
            mirrorLevel.getChunk(room.chunkX(), room.chunkZ());
            placeInnerMirror(mirrorLevel, room, sourceMirror.isDemonKilled());
        });
    }

    private static Optional<MirrorCarrier> findCarriedMirror(MinecraftServer server, int roomIndex,
                                                              ServerPlayer traveler) {
        for (ServerLevel level : server.getAllLevels()) {
            if (WitcheryDimensions.isMirrorWorld(level)) continue;
            for (ServerPlayer candidate : level.players()) {
                if (candidate == traveler) continue;
                for (int slot = 0; slot < candidate.getInventory().getContainerSize(); slot++) {
                    if (MirrorItem.roomIndex(candidate.getInventory().getItem(slot)) == roomIndex) {
                        return Optional.of(new MirrorCarrier(candidate));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static BlockPos findRoomEntry(ServerLevel level, int chunkX, int sectionY, int chunkZ) {
        BlockPos mirrorFoot = MirrorWorldLayout.innerMirrorFoot(chunkX, sectionY, chunkZ);
        BlockPos sourceLanding = mirrorFoot.relative(Direction.EAST);
        return hasClearBody(level, sourceLanding)
                ? sourceLanding
                : MirrorWorldLayout.roomCenter(chunkX, sectionY, chunkZ);
    }

    private static void spawnRoomGuardian(ServerLevel level, MirrorRoomData.Room room,
                                          ServerPlayer player) {
        MirrorRoomData roomData = MirrorRoomData.get(level.getServer());
        AABB roomBounds = roomBounds(room.chunkX(), room.sectionY(), room.chunkZ());
        for (ReflectionEntity reflection : level.getEntitiesOfClass(ReflectionEntity.class, roomBounds)) {
            if (reflection.isAlive() && !reflection.isRemoved()
                    && reflection.isRoomGuardianFor(room.index())) {
                roomData.setGuardian(room.index(), reflection.getUUID());
                return;
            }
        }
        if (roomData.roomAtIndex(room.index()).flatMap(MirrorRoomData.Room::guardian).isPresent()) {
            return;
        }

        BlockPos center = MirrorWorldLayout.roomCenter(room.chunkX(), room.sectionY(), room.chunkZ());
        ReflectionEntity reflection = new ReflectionEntity(WitcheryEntities.REFLECTION.get(), level);
        reflection.snapTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        reflection.setOwner(player);
        reflection.setGuardianRoom(room.index());
        reflection.setPersistenceRequired();
        if (level.addFreshEntity(reflection)) {
            roomData.setGuardian(room.index(), reflection.getUUID());
        }
    }

    /**
     * Re-evaluates the allocated room containing a defeated Reflection. The source keeps the
     * mirror inhabited until every Reflection in that room is dead, including freely spawned
     * Reflections that wandered into it.
     */
    public static boolean clearReflectionRoom(ServerLevel mirrorLevel, ReflectionEntity defeated) {
        if (!WitcheryDimensions.isMirrorWorld(mirrorLevel)) {
            return false;
        }
        BlockPos pos = defeated.blockPosition();
        int chunkX = Math.floorDiv(pos.getX(), 16);
        int sectionY = MirrorWorldLayout.sectionY(pos.getY());
        int chunkZ = Math.floorDiv(pos.getZ(), 16);
        return MirrorRoomData.get(mirrorLevel.getServer()).roomAt(chunkX, sectionY, chunkZ)
                .map(room -> clearGuardianRoom(mirrorLevel, defeated, room.index()))
                .orElse(false);
    }

    /** Called after a Reflection death to clear only its exact source mirror. */
    public static boolean clearGuardianRoom(ServerLevel mirrorLevel, ReflectionEntity defeated,
                                            int roomIndex) {
        MirrorRoomData roomData = MirrorRoomData.get(mirrorLevel.getServer());
        Optional<MirrorRoomData.Room> room = roomData.roomAtIndex(roomIndex);
        if (room.isEmpty()) {
            return false;
        }
        MirrorWorldLayout.RoomCoordinates coordinates = MirrorWorldLayout.roomForIndex(roomIndex);
        for (ReflectionEntity reflection : mirrorLevel.getEntitiesOfClass(
                ReflectionEntity.class, roomBounds(coordinates.chunkX(), coordinates.sectionY(), coordinates.chunkZ()))) {
            if (reflection != defeated && reflection.isAlive()) {
                reflection.setGuardianRoom(roomIndex);
                reflection.setPersistenceRequired();
                roomData.setGuardian(roomIndex, reflection.getUUID());
                return false;
            }
        }

        roomData.setGuardian(roomIndex, null);

        MirrorBlockEntity innerMirror = getMirrorBlockEntity(mirrorLevel,
                MirrorWorldLayout.innerMirrorFoot(coordinates.chunkX(), coordinates.sectionY(), coordinates.chunkZ()));
        if (innerMirror != null) {
            innerMirror.setDemonKilled(true);
        }

        ServerLevel sourceLevel = resolveLevelByKey(mirrorLevel.getServer(), room.get().sourceDimension());
        if (sourceLevel == null) {
            return false;
        }
        MirrorBlockEntity sourceMirror = getMirrorBlockEntity(sourceLevel, room.get().sourceBlockPos());
        if (sourceMirror == null) {
            return false;
        }
        sourceMirror.setDemonKilled(true);
        return true;
    }

    public static AABB roomBounds(int chunkX, int sectionY, int chunkZ) {
        int minX = chunkX * 16 + MirrorWorldLayout.INTERIOR_MIN;
        int minZ = chunkZ * 16 + MirrorWorldLayout.INTERIOR_MIN;
        int minY = MirrorWorldLayout.roomFloorY(sectionY);
        return new AABB(minX, minY, minZ,
                chunkX * 16 + MirrorWorldLayout.INTERIOR_MAX + 1,
                MirrorWorldLayout.roomCeilingY(sectionY),
                chunkZ * 16 + MirrorWorldLayout.INTERIOR_MAX + 1);
    }

    public static BlockPos mirrorFoot(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(WitcheryBlocks.MIRROR.get()) && state.hasProperty(MirrorBlock.PART)
                && state.getValue(MirrorBlock.PART) == BedPart.HEAD ? pos.below() : pos;
    }

    public static MirrorBlockEntity getMirrorBlockEntity(ServerLevel level, BlockPos pos) {
        BlockPos foot = mirrorFoot(level, pos);
        return level.getBlockState(foot).is(WitcheryBlocks.MIRROR.get())
                && level.getBlockEntity(foot) instanceof MirrorBlockEntity mirror ? mirror : null;
    }

    public static BlockPos findLandingBesideMirror(ServerLevel level, BlockPos mirrorPos) {
        BlockPos foot = mirrorFoot(level, mirrorPos);
        BlockState state = level.getBlockState(foot);
        if (state.is(WitcheryBlocks.MIRROR.get()) && state.hasProperty(MirrorBlock.FACING)) {
            BlockPos facingLanding = foot.relative(state.getValue(MirrorBlock.FACING));
            if (isSafeStand(level, facingLanding)) {
                return facingLanding;
            }
        }
        return findSafeLanding(level, foot, 4);
    }

    public static BlockPos findSafeLanding(ServerLevel level, BlockPos origin, int radius) {
        for (int distance = 0; distance <= radius; distance++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -distance; dx <= distance; dx++) {
                    for (int dz = -distance; dz <= distance; dz++) {
                        if (distance > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != distance) {
                            continue;
                        }
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (isSafeStand(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeStand(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinY() || feet.getY() + 1 >= level.getMaxY()) {
            return false;
        }
        BlockPos floor = feet.below();
        return !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                && hasClearBody(level, feet);
    }

    private static boolean hasClearBody(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static boolean teleport(ServerPlayer player, ServerLevel destination, BlockPos feet,
                                    float yaw, float pitch) {
        double x = feet.getX() + 0.5D;
        double y = feet.getY();
        double z = feet.getZ() + 0.5D;
        if (player.level() == destination) {
            player.teleportTo(x, y, z);
        } else {
            player.teleportTo(destination, x, y, z, Set.of(), yaw, pitch, false);
        }
        // ServerPlayer teleport placement is finalized by the connection at the end of the tick.
        return player.level() == destination;
    }

    public static ServerLevel resolveLevelByKey(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    private record MirrorCarrier(ServerPlayer player) {}
}
