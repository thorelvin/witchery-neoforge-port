package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public class CreativeBatWolfTokenItem extends Item {
    private static final int USE_DURATION = 20;
    private static final int MAX_VAMPIRE_LEVEL = 10;
    private static final int MAX_WOLF_LEVEL = 10;

    public CreativeBatWolfTokenItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || remainingUseDuration != 1 || !(entity instanceof ServerPlayer player)) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (player.isShiftKeyDown()) {
            cycleVampire(player, data);
        } else {
            cycleWolf(player, data);
        }
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        feedback((ServerLevel) level, player);
    }

    private static void cycleVampire(ServerPlayer player, WitcheryPlayerData data) {
        int next = data.getVampireLevel() + 1;
        if (next > MAX_VAMPIRE_LEVEL) {
            next = 0;
        }

        if (next == 0) {
            data.setVampire(false);
            data.setBloodLevel(0);
        } else {
            data.setVampire(true);
            data.increaseVampireLevelCap(next);
            data.setVampireLevel(next);
        }

        player.displayClientMessage(Component.translatable(
                "item.witchery.creative_bat_wolf_token.vampire", next).withStyle(ChatFormatting.DARK_RED), false);
    }

    private static void cycleWolf(ServerPlayer player, WitcheryPlayerData data) {
        int next = data.isWerewolf() ? data.getWerewolfLevel() + 1 : 1;
        if (next > MAX_WOLF_LEVEL) {
            next = 0;
        }

        data.setWerewolfLevel(next);
        if (next == 0) {
            data.setTransformationLevel(0.0F);
        }

        player.displayClientMessage(Component.translatable(
                "item.witchery.creative_bat_wolf_token.werewolf", next).withStyle(ChatFormatting.GOLD), false);
    }

    private static void feedback(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.7F, 1.2F);
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                8, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        String tipKey = "item.witchery.creative_medallion".equals(getDescriptionId())
                ? "item.witchery.creative_medallion.tip"
                : "item.witchery.creative_bat_wolf_token.tip";
        tooltip.accept(Component.translatable(tipKey)
                .withStyle(ChatFormatting.GRAY));
    }
}
