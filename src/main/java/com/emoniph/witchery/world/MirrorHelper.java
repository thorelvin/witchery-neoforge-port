package com.emoniph.witchery.world;

import com.emoniph.witchery.blocks.MirrorBlock;
import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/**
 * Handles Mirror World entry and exit teleportation.
 */
public class MirrorHelper {

    public static final int ROOM_FLOOR_Y = 6;

    /**
     * Called when a player touches the Mirror block from outside the Mirror World.
     */
    public static void enterMirrorWorld(ServerPlayer player, ServerLevel overworld,
                                        BlockPos mirrorPos) {
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return;

        MirrorBlockEntity be = getMirrorBE(overworld, mirrorPos);
        if (be == null) return;

        MinecraftServer server = overworld.getServer();
        ServerLevel mirrorLevel = server.getLevel(WitcheryDimensions.MIRROR_WORLD);
        if (mirrorLevel == null) return;

        if (!be.hasRoom()) {
            MirrorRoomData data = MirrorRoomData.get(overworld);
            int[] room = data.allocateRoom();
            be.setRoom(room[0], room[1]);

            String dimId = overworld.dimension().identifier().toString();
            placeInnerMirror(mirrorLevel, room[0], room[1], mirrorPos, dimId);
        }

        int[] room = be.getRoom();
        int entryX = room[0] * 16 + 8;
        int entryZ = room[1] * 16 + 8;

        mirrorLevel.getChunk(room[0], room[1]);

        // Find the first open Y starting from the expected floor, scanning up through the room ceiling
        int entryY = ROOM_FLOOR_Y;
        for (int y = ROOM_FLOOR_Y; y <= ROOM_FLOOR_Y + 9; y++) {
            BlockPos checkPos = new BlockPos(entryX, y, entryZ);
            if (mirrorLevel.getBlockState(checkPos).isAir()
                    && mirrorLevel.getBlockState(checkPos.above()).isAir()) {
                entryY = y;
                break;
            }
        }

        saveReturnPos(player, overworld, mirrorPos);
        teleport(player, mirrorLevel, entryX + 0.5, (double) entryY, entryZ + 0.5, 180f, 0f);

        if (!be.isDemonKilled()) {
            spawnReflection(mirrorLevel, entryX, entryY, entryZ, player);
        }
    }

    /**
     * Called when a player touches the inner (unbreakable) Mirror from inside the Mirror World.
     * Resolves the linked dimension so deep mirror rooms (mirror-world → mirror-world) work.
     */
    public static void exitMirrorWorld(ServerPlayer player, ServerLevel mirrorLevel,
                                       BlockPos innerMirrorPos) {
        if (!player.level().dimension().equals(WitcheryDimensions.MIRROR_WORLD)) return;

        MirrorBlockEntity be = getMirrorBE(mirrorLevel, innerMirrorPos);
        if (be == null) return;

        MinecraftServer server = mirrorLevel.getServer();
        BlockPos outerPos = be.getLinkedPos();
        if (outerPos == null) {
            ServerLevel fallback = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (fallback != null) teleport(player, fallback, 0.5, 64.0, 0.5, 0f, 0f);
            return;
        }

        String linkedDimStr = be.getLinkedDim();
        ServerLevel destLevel = linkedDimStr != null
                ? resolveLevelByKey(server, linkedDimStr)
                : server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (destLevel == null) return;

        double tx = outerPos.getX() + 0.5;
        double ty = (double) outerPos.getY();
        double tz = outerPos.getZ() + 0.5;

        BlockState outerState = destLevel.getBlockState(outerPos);
        if (outerState.hasProperty(MirrorBlock.FACING)) {
            Direction face = outerState.getValue(MirrorBlock.FACING);
            tx += face.getStepX();
            tz += face.getStepZ();
        }

        teleport(player, destLevel, tx, ty, tz, 0f, 0f);
    }

    /**
     * Called when a player inside the Mirror World walks through a regular (breakable) mirror
     * they placed there. Allocates a new nested room and teleports into it.
     */
    public static void enterDeepMirrorRoom(ServerPlayer player, ServerLevel mirrorLevel,
                                           BlockPos innerMirrorPos) {
        if (!WitcheryDimensions.isMirrorWorld(player.level())) return;

        MirrorBlockEntity be = getMirrorBE(mirrorLevel, innerMirrorPos);
        if (be == null) return;

        MinecraftServer server = mirrorLevel.getServer();
        // MirrorRoomData is always stored on the overworld level
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;

        if (!be.hasRoom()) {
            MirrorRoomData data = MirrorRoomData.get(overworld);
            int[] room = data.allocateRoom();
            be.setRoom(room[0], room[1]);

            String outerDimId = WitcheryDimensions.MIRROR_WORLD.identifier().toString();
            placeInnerMirror(mirrorLevel, room[0], room[1], innerMirrorPos, outerDimId);
        }

        int[] room = be.getRoom();
        int entryX = room[0] * 16 + 8;
        int entryZ = room[1] * 16 + 8;

        mirrorLevel.getChunk(room[0], room[1]);

        int entryY = ROOM_FLOOR_Y;
        for (int y = ROOM_FLOOR_Y; y <= ROOM_FLOOR_Y + 9; y++) {
            BlockPos checkPos = new BlockPos(entryX, y, entryZ);
            if (mirrorLevel.getBlockState(checkPos).isAir()
                    && mirrorLevel.getBlockState(checkPos.above()).isAir()) {
                entryY = y;
                break;
            }
        }

        teleport(player, mirrorLevel, entryX + 0.5, (double) entryY, entryZ + 0.5, 180f, 0f);

        if (!be.isDemonKilled()) {
            spawnReflection(mirrorLevel, entryX, entryY, entryZ, player);
        }
    }

    // ── Internals ──────────────────────────────────────────────────────────

    private static void placeInnerMirror(ServerLevel mirrorLevel,
                                          int roomCX, int roomCZ,
                                          BlockPos outerPos, String outerDimId) {
        int mx = roomCX * 16 + 4;
        int my = ROOM_FLOOR_Y + 1;
        int mz = roomCZ * 16 + 8;

        BlockPos bottomPos = new BlockPos(mx, my, mz);
        BlockPos topPos    = bottomPos.above();

        BlockState bottomState = WitcheryBlocks.MIRROR.get().defaultBlockState()
                .setValue(MirrorBlock.FACING, Direction.EAST)
                .setValue(MirrorBlock.PART,   BedPart.FOOT)
                .setValue(MirrorBlock.UNBREAKABLE, true);
        BlockState topState = bottomState.setValue(MirrorBlock.PART, BedPart.HEAD);

        mirrorLevel.setBlockAndUpdate(bottomPos, bottomState);
        mirrorLevel.setBlockAndUpdate(topPos,    topState);

        MirrorBlockEntity be = getMirrorBE(mirrorLevel, bottomPos);
        if (be != null) {
            be.setLinkedPos(outerPos);
            be.setLinkedDim(outerDimId);
            be.setDemonKilled(false);
        }
    }

    private static void spawnReflection(ServerLevel level, int x, int y, int z, ServerPlayer player) {
        boolean exists = !level.getEntitiesOfClass(
                ReflectionEntity.class,
                new AABB(x - 8, y - 4, z - 8, x + 8, y + 12, z + 8)
        ).isEmpty();
        if (exists) return;

        ReflectionEntity reflection = new ReflectionEntity(WitcheryEntities.REFLECTION.get(), level);
        reflection.snapTo((double) x + 0.5, (double) y, (double) z + 0.5, 0f, 0f);
        reflection.setOwner(player);
        level.addFreshEntity(reflection);
    }

    private static void saveReturnPos(ServerPlayer player, ServerLevel level, BlockPos mirrorPos) {
        player.getPersistentData().putInt("MirrorReturnX", mirrorPos.getX());
        player.getPersistentData().putInt("MirrorReturnY", mirrorPos.getY());
        player.getPersistentData().putInt("MirrorReturnZ", mirrorPos.getZ());
    }

    private static void teleport(ServerPlayer player, ServerLevel dest,
                                  double x, double y, double z, float yaw, float pitch) {
        if (player.level() == dest) {
            player.teleportTo(x, y, z);
        } else {
            player.teleportTo(dest, x, y, z, Set.of(), yaw, pitch, false);
        }
    }

    private static MirrorBlockEntity getMirrorBE(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(WitcheryBlocks.MIRROR.get())) return null;
        BlockPos foot = (state.hasProperty(MirrorBlock.PART) && state.getValue(MirrorBlock.PART) == BedPart.HEAD)
                ? pos.below() : pos;
        if (level.getBlockEntity(foot) instanceof MirrorBlockEntity be) return be;
        return null;
    }

    private static ServerLevel resolveLevelByKey(MinecraftServer server, String dimKey) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimKey)) {
                return level;
            }
        }
        return null;
    }
}
