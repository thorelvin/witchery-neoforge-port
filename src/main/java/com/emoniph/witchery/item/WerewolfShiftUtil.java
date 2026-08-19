package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    /** Source isWolfAnimalForm: transformed into either wolf shape. */
    public static boolean inWolfShape(WitcheryPlayerData data) {
        return isTransformed(data) && data.getWolfForm() != WolfForm.NONE;
    }

    public static boolean inWolfmanForm(WitcheryPlayerData data) {
        return isTransformed(data) && data.getWolfForm() == WolfForm.WOLFMAN;
    }

    /** Strictly the wolf (four-legged) shape — source TransformCreature.WOLF. */
    public static boolean inWolfForm(WitcheryPlayerData data) {
        return isTransformed(data) && data.getWolfForm() == WolfForm.WOLF;
    }

    /** Source canControlTransform: voluntary shifting unlocks at werewolf level 2. */
    public static boolean canControlTransform(WitcheryPlayerData data) {
        return data.getWerewolfLevel() >= WerewolfProgression.CONTROL_LEVEL;
    }

    /** Source isWolfmanAllowed: the wolfman (biped) shape unlocks at werewolf level 5. */
    public static boolean isWolfmanAllowed(WitcheryPlayerData data) {
        return data.getWerewolfLevel() >= WerewolfProgression.WOLFMAN_LEVEL;
    }

    public static boolean hasMoonCharm(Player player) {
        return hasItemInInventory(player, WitcheryItems.MOON_CHARM.get());
    }

    public static boolean isWolfsbaneActive(Player player, WitcheryPlayerData data) {
        var effect = player.getEffect(WitcheryMobEffects.WOLFSBANE);
        return effect != null
                && WerewolfProgression.wolfsbaneStrength(effect.getAmplifier()) >= data.getWerewolfLevel();
    }

    public static void setTransformed(ServerPlayer player, WitcheryPlayerData data,
                                      boolean transformed, Component message) {
        shiftTo(player, data, transformed ? WolfForm.WOLF : WolfForm.NONE, message);
    }

    public static void shiftTo(ServerPlayer player, WitcheryPlayerData data,
                               WolfForm form, Component message) {
        WolfForm target = form == null ? WolfForm.NONE : form;
        data.setTransformationLevel(target == WolfForm.NONE ? 0.0F : 1.0F);
        data.setWolfForm(target);
        player.refreshDimensions();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                16, 1.0D, 1.0D, 1.0D, 0.0D);
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
