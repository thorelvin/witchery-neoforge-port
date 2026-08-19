package com.emoniph.witchery.world;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * Helpers for entering and exiting the Torment dimension.
 *
 * Torment uses six fixed maze levels stacked vertically. Players always enter
 * at the legacy maze start near (8, standingY, 8), and the phase-2 Lord of
 * Torment appears deeper in the maze near (9, standingY, 36).
 */
public final class TormentHelper {

    public static final int NUM_LEVELS   = 6;
    public static final int BASE_LEVEL   = 10;
    public static final int LEVEL_HEIGHT = 15;
    public static final int MAZE_SIZE    = 31;
    public static final int ENTRY_X      = 8;
    public static final int ENTRY_Z      = 8;
    public static final int BOSS_X       = 9;
    public static final int BOSS_Z       = 36;
    public static final int ORIGIN_X     = ENTRY_X - MAZE_SIZE;
    public static final int ORIGIN_Z     = ENTRY_Z - 2;
    public static final int PORTAL_X     = ORIGIN_X + MAZE_SIZE;
    public static final int PORTAL_Z     = ORIGIN_Z + MAZE_SIZE * 2;
    public static final int STRUCTURE_MIN_X = ORIGIN_X;
    public static final int STRUCTURE_MAX_X = ORIGIN_X + MAZE_SIZE * 2;
    public static final int STRUCTURE_MIN_Z = ORIGIN_Z;
    public static final int STRUCTURE_MAX_Z = ORIGIN_Z + MAZE_SIZE * 2 + 1;

    private TormentHelper() {}

    public static int getMazeY(int level) {
        return BASE_LEVEL + level * LEVEL_HEIGHT;
    }

    public static int getStandingY(int level) {
        return getMazeY(level) + 1;
    }

    public static BlockPos getEntryPos(int level) {
        // Source WorldProviderTorment places arrivals one block above the normal passage lane.
        return new BlockPos(ENTRY_X, getMazeY(level) + 2, ENTRY_Z);
    }

    public static BlockPos getBossPos(int level) {
        return new BlockPos(BOSS_X, getStandingY(level), BOSS_Z);
    }

    public static BlockPos getPortalLowerPos(int level) {
        return new BlockPos(PORTAL_X, getStandingY(level), PORTAL_Z);
    }

    public static BlockPos getPortalUpperPos(int level) {
        return getPortalLowerPos(level).above();
    }

    public static boolean sendToTorment(ServerPlayer player, int level) {
        return sendToTorment(player, level, null);
    }

    public static boolean sendToTorment(ServerPlayer player, int level, UUID sessionId) {
        level = Math.clamp(level, 0, NUM_LEVELS - 1);
        ServerLevel torment = ((ServerLevel) player.level()).getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) return false;

        var data = WitcheryAttachments.get(player);
        boolean entering = !player.level().dimension().equals(WitcheryDimensions.TORMENT);
        BlockPos returnPos = entering ? player.blockPosition() : null;
        ResourceKey<Level> returnDimension = entering ? player.level().dimension() : null;

        BlockPos entryPos = getEntryPos(level);

        // Force generation before refreshing the known legacy entry pocket.
        torment.getChunk(0, 0);
        torment.getChunkAt(entryPos);
        for (int x = ENTRY_X - 1; x <= ENTRY_X + 1; x++) {
            for (int z = ENTRY_Z - 1; z <= ENTRY_Z + 1; z++) {
                for (int y = entryPos.getY(); y <= entryPos.getY() + 1; y++) {
                    torment.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        ServerLevel sourceLevel = (ServerLevel) player.level();
        playTravelEffects(sourceLevel, player.position());
        player.stopRiding();
        player.ejectPassengers();
        Entity moved = player.teleport(new TeleportTransition(
                torment,
                Vec3.atBottomCenterOf(entryPos),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                Set.<Relative>of(),
                TeleportTransition.DO_NOTHING));
        if (moved == null) return false;
        moved.snapTo(entryPos.getX() + 0.5D, entryPos.getY(), entryPos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        playTravelEffects(torment, moved.position());

        if (entering) {
            data.setTormentReturn(returnPos, returnDimension, sessionId);
        } else if (sessionId != null) {
            data.setTormentSession(sessionId);
        }
        return true;
    }

    public static boolean returnFromTorment(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        BlockPos returnPos = data.getTormentReturnPos().orElse(null);
        String returnDimStr = data.getTormentReturnDimension();
        boolean migratedAnchor = false;
        if (returnPos == null && returnDimStr.isBlank()) {
            // Players already inside Torment on upgrade used the old shared otherworld anchor.
            returnPos = data.getReturnPos().orElse(null);
            returnDimStr = data.getReturnDimension();
            migratedAnchor = returnPos != null || !returnDimStr.isBlank();
        }

        ServerLevel returnLevel = resolveLevel(player, returnDimStr);

        if (returnPos == null) {
            returnPos = new BlockPos(0, 64, 0);
        }

        BlockPos safe = findSafe(returnLevel, returnPos);
        UUID session = data.getTormentSession().orElse(null);
        playTravelEffects((ServerLevel) player.level(), player.position());
        player.stopRiding();
        player.ejectPassengers();
        Entity moved = player.teleport(new TeleportTransition(
                returnLevel,
                Vec3.atBottomCenterOf(safe),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                Set.<Relative>of(),
                TeleportTransition.DO_NOTHING));
        if (moved == null) return false;
        moved.snapTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        playTravelEffects(returnLevel, moved.position());

        data.clearTormentReturn();
        if (migratedAnchor) data.clearReturnPos();
        if (session != null) {
            TormentReleaseData.get(player.level().getServer()).consume(player.getUUID(), session);
        }
        return true;
    }

    /** Completes a persisted phase-two release when a participant reconnects after the boss died. */
    public static boolean releaseIfDefeated(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        UUID session = data.getTormentSession().orElse(null);
        if (session == null) return false;
        TormentReleaseData releases = TormentReleaseData.get(player.level().getServer());
        boolean released = releases.isReleased(session, player.getUUID());
        if (released) {
            if (player.level().dimension().equals(WitcheryDimensions.TORMENT)) {
                return returnFromTorment(player);
            }
            releases.consume(player.getUUID(), session);
            data.clearTormentReturn();
        }
        return false;
    }

    /** Clears stale Torment state after a death respawns the player outside the dimension. */
    public static void abandonSession(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        UUID session = data.getTormentSession().orElse(null);
        if (session != null) {
            TormentReleaseData.get(player.level().getServer()).consume(player.getUUID(), session);
        }
        data.clearTormentReturn();
    }

    private static ServerLevel resolveLevel(ServerPlayer player, String dimStr) {
        if (!dimStr.isEmpty()) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            ServerLevel found = ((ServerLevel) player.level()).getServer().getLevel(key);
            if (found != null) return found;
        }
        ServerLevel ow = ((ServerLevel) player.level()).getServer().getLevel(Level.OVERWORLD);
        return ow != null ? ow : (ServerLevel) player.level();
    }

    private static void playTravelEffects(ServerLevel level, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.PORTAL,
                position.x, position.y + 1.0D, position.z,
                16, 1.0D, 2.0D, 1.0D, 0.05D);
    }

    private static BlockPos findSafe(ServerLevel level, BlockPos target) {
        for (int dy = 0; dy < 64; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos check = target.above(dy * sign);
                if (level.getBlockState(check).isSolid()
                        && level.getBlockState(check.above()).isAir()
                        && level.getBlockState(check.above(2)).isAir()) {
                    return check.above();
                }
            }
        }
        return target;
    }
}
