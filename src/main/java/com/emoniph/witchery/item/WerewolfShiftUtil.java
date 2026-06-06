package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WerewolfShiftUtil {
    private WerewolfShiftUtil() {
    }

    public static boolean isTransformed(WitcheryPlayerData data) {
        return data.getTransformationLevel() > 0.01F;
    }

    public static boolean hasMoonCharm(Player player) {
        return hasItemInInventory(player, WitcheryItems.MOON_CHARM.get());
    }

    public static boolean hasWolfsbane(Player player) {
        return player.hasEffect(WitcheryMobEffects.WOLFSBANE)
                || hasItemInInventory(player, WitcheryItems.WOLFSBANE_FLOWER.get());
    }

    public static void setTransformed(ServerPlayer player, WitcheryPlayerData data,
                                      boolean transformed, Component message) {
        data.setTransformationLevel(transformed ? 1.0F : 0.0F);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(),
                transformed ? SoundEvents.WOLF_SHAKE : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, transformed ? 0.8F : 0.6F, transformed ? 0.8F : 1.4F);
        level.sendParticles(transformed ? ParticleTypes.EXPLOSION : ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                transformed ? 16 : 12, transformed ? 1.0D : 0.4D, transformed ? 1.0D : 0.5D,
                transformed ? 1.0D : 0.4D, transformed ? 0.0D : 0.01D);
        if (message != null) {
            player.displayClientMessage(message, false);
        }
    }

    private static boolean hasItemInInventory(Player player, Item item) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }
}
