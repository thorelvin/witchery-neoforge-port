package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class MoonCharmItem extends Item {
    private static final int SOURCE_DURABILITY = 49;
    private static final int USE_DURATION = 60;

    public MoonCharmItem(Properties properties) {
        super(properties.stacksTo(1)
                .durability(SOURCE_DURABILITY)
                .repairable(Items.GOLD_NUGGET)
                .rarity(Rarity.RARE));
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
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int masteryLevel = Math.max(1, data.getWolfMastery() + 1);
        int triggerRemaining = Math.max((masteryLevel - 1) * 4, 1);
        if (remainingUseDuration != triggerRemaining) {
            return;
        }

        if (!data.isWerewolf() || WerewolfShiftUtil.hasWolfsbane(player)) {
            fail(player);
        } else {
            boolean transformed = !WerewolfShiftUtil.isTransformed(data);
            WerewolfShiftUtil.setTransformed(player, data, transformed, transformed
                    ? Component.translatable("item.witchery.moon_charm.transformed").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("item.witchery.moon_charm.restored").withStyle(ChatFormatting.GRAY));
        }

        EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, player, slot);
    }

    private static void fail(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS, 0.5F, 0.6F);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                12, 0.4D, 0.5D, 0.4D, 0.01D);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.moon_charm.tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
