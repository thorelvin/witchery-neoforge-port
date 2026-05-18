package com.emoniph.witchery.world;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/**
 * Helpers for entering and exiting the Torment dimension.
 *
 * Six maze levels stacked vertically:
 *   level L → mazeY = BASE_LEVEL + L * LEVEL_HEIGHT
 *           → entryY = mazeY + 2  (player stands here on entry)
 *
 * Entry point is always (8, entryY, 8).  A 3×3×2 space is cleared around it
 * so the player is never stuck inside a wall regardless of maze layout.
 */
public final class TormentHelper {

    public static final int NUM_LEVELS   = 6;
    public static final int BASE_LEVEL   = 10;
    public static final int LEVEL_HEIGHT = 15;

    private TormentHelper() {}

    public static int getMazeY(int level) {
        return BASE_LEVEL + level * LEVEL_HEIGHT;
    }

    public static int getEntryY(int level) {
        return getMazeY(level) + 2;
    }

    // ── Entry ─────────────────────────────────────────────────────────────────

    public static void sendToTorment(ServerPlayer player, int level) {
        level = Math.clamp(level, 0, NUM_LEVELS - 1);
        ServerLevel torment = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) return;

        // Save overworld return position (only overwrite if not already in Torment)
        if (!player.level().dimension().equals(WitcheryDimensions.TORMENT)) {
            var data = WitcheryAttachments.get(player);
            data.setReturnPos(player.blockPosition(), player.level().dimension());
        }

        int entryY = getEntryY(level);

        // Force chunk generation first so the maze exists
        torment.getChunk(0, 0);

        // Clear a 3×3×2 entry pocket so the player always has room to stand
        for (int x = 7; x <= 9; x++)
            for (int z = 7; z <= 9; z++) {
                torment.setBlock(new BlockPos(x, entryY,     z), Blocks.AIR.defaultBlockState(), 3);
                torment.setBlock(new BlockPos(x, entryY + 1, z), Blocks.AIR.defaultBlockState(), 3);
            }

        player.teleportTo(torment, 8.5, (double)(entryY), 8.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
    }

    // ── Exit ──────────────────────────────────────────────────────────────────

    public static void returnFromTorment(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        BlockPos returnPos = data.getReturnPos().orElse(null);
        String returnDimStr = data.getReturnDimension();
        data.clearReturnPos();

        ServerLevel returnLevel = resolveLevel(player, returnDimStr);

        if (returnPos == null) {
            returnPos = new net.minecraft.core.BlockPos(0, 64, 0);
        }

        // Search for a safe Y
        BlockPos safe = findSafe(returnLevel, returnPos);

        player.teleportTo(returnLevel, safe.getX() + 0.5, (double)(safe.getY()), safe.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ServerLevel resolveLevel(ServerPlayer player, String dimStr) {
        if (!dimStr.isEmpty()) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            ServerLevel found = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(key);
            if (found != null) return found;
        }
        ServerLevel ow = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(Level.OVERWORLD);
        return ow != null ? ow : ((net.minecraft.server.level.ServerLevel) player.level());
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
