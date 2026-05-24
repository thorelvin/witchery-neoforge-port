package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RubySlippersItem extends Item {
    private static final long COOLDOWN_TICKS = 36000L; // 30 minutes
    private static final Map<UUID, Long> lastUsedTick = new HashMap<>();

    public RubySlippersItem(Properties properties) {
        super(properties);
    }

    public static boolean tryActivate(ServerPlayer player, String message) {
        if (!message.toLowerCase().replace("'", "").startsWith("theres no place like home")) return false;

        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(feet.getItem() instanceof RubySlippersItem)) return false;
        if (player.level().dimension().equals(com.emoniph.witchery.init.WitcheryDimensions.SPIRIT_WORLD)) return false;

        long now = player.level().getGameTime();
        long last = lastUsedTick.getOrDefault(player.getUUID(), 0L);
        if (now - last < COOLDOWN_TICKS) {
            int minutesLeft = (int) Math.max(1, (COOLDOWN_TICKS - (now - last)) / 1200L);
            player.displayClientMessage(Component.translatable(
                    "witchery.item.ruby_slippers.cooldown", minutesLeft), false);
            return true;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) return true;

        ServerLevel dest = server.getLevel(Level.OVERWORLD);
        BlockPos target = null;

        // Try bed/respawn position via 1.21.1 API
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig != null && respawnConfig.respawnData() != null) {
            ServerLevel respawnDim = server.getLevel(respawnConfig.respawnData().dimension());
            if (respawnDim != null) {
                dest = respawnDim;
                target = respawnConfig.respawnData().pos();
            }
        }

        if (dest == null) return true;
        if (target == null) {
            // Fall back to overworld spawn
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) return true;
            dest = overworld;
            target = new BlockPos(0, 64, 0);
        }

        lastUsedTick.put(player.getUUID(), now);
        BlockPos safe = findSafePos(dest, target);
        player.teleportTo(dest, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        player.displayClientMessage(Component.translatable("witchery.item.ruby_slippers.activated"), false);
        return true;
    }

    private static BlockPos findSafePos(ServerLevel level, BlockPos start) {
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos candidate = start.above(dy);
            if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return start.above();
    }
}
