package com.emoniph.witchery.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class WitcheryTerrainProtection {
    private WitcheryTerrainProtection() {
    }

    public static boolean canBreak(@Nullable Entity actor, Level level, BlockPos pos) {
        return canModify(actor, level, pos, WitcheryProtectionAction.BLOCK_BREAK);
    }

    public static boolean canPlace(@Nullable Entity actor, Level level, BlockPos pos) {
        return canModify(actor, level, pos, WitcheryProtectionAction.BLOCK_PLACE);
    }

    public static boolean canTransform(@Nullable Entity actor, Level level, BlockPos pos) {
        return canModify(actor, level, pos, WitcheryProtectionAction.BLOCK_TRANSFORM);
    }

    public static boolean canModify(@Nullable Entity actor, Level level, BlockPos pos,
                                    WitcheryProtectionAction action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        return WitcheryProtectionService.canModifyBlock(serverPlayer(actor, serverLevel), serverLevel, pos, action);
    }

    @Nullable
    public static ServerPlayer serverPlayer(@Nullable Entity actor, ServerLevel level) {
        if (actor instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        if (actor instanceof Player player) {
            return level.getServer().getPlayerList().getPlayer(player.getUUID());
        }
        return null;
    }
}
