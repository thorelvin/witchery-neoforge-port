package com.emoniph.witchery.world;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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
    public static final int ENTRY_X      = 8;
    public static final int ENTRY_Z      = 8;
    public static final int BOSS_X       = 9;
    public static final int BOSS_Z       = 36;

    private TormentHelper() {}

    public static int getMazeY(int level) {
        return BASE_LEVEL + level * LEVEL_HEIGHT;
    }

    public static int getStandingY(int level) {
        return getMazeY(level) + 1;
    }

    public static BlockPos getEntryPos(int level) {
        return new BlockPos(ENTRY_X, getStandingY(level), ENTRY_Z);
    }

    public static BlockPos getBossPos(int level) {
        return new BlockPos(BOSS_X, getStandingY(level), BOSS_Z);
    }

    public static void sendToTorment(ServerPlayer player, int level) {
        level = Math.clamp(level, 0, NUM_LEVELS - 1);
        ServerLevel torment = ((ServerLevel) player.level()).getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) return;

        if (!player.level().dimension().equals(WitcheryDimensions.TORMENT)) {
            var data = WitcheryAttachments.get(player);
            data.setReturnPos(player.blockPosition(), player.level().dimension());
        }

        BlockPos entryPos = getEntryPos(level);

        // Force generation before refreshing the known legacy entry pocket.
        torment.getChunk(0, 0);
        torment.getChunkAt(entryPos);
        for (int x = ENTRY_X - 1; x <= ENTRY_X + 1; x++) {
            for (int z = ENTRY_Z - 1; z <= ENTRY_Z + 1; z++) {
                for (int y = entryPos.getY(); y <= entryPos.getY() + 3; y++) {
                    torment.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        player.teleportTo(torment, entryPos.getX() + 0.5, (double) entryPos.getY(), entryPos.getZ() + 0.5,
                java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
    }

    public static void returnFromTorment(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        BlockPos returnPos = data.getReturnPos().orElse(null);
        String returnDimStr = data.getReturnDimension();
        data.clearReturnPos();

        ServerLevel returnLevel = resolveLevel(player, returnDimStr);

        if (returnPos == null) {
            returnPos = new BlockPos(0, 64, 0);
        }

        BlockPos safe = findSafe(returnLevel, returnPos);
        player.teleportTo(returnLevel, safe.getX() + 0.5, (double) safe.getY(), safe.getZ() + 0.5,
                java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
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
