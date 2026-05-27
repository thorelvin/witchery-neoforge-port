package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class DeathsHandItem extends Item {
    public static final String TAG_SCYTHE_DEPLOYED = "WITCScytheDeployed";

    public DeathsHandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!DeathsClothingItem.isFullSetWorn(player)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean deployed = !isDeployed(stack);
        setDeployed(stack, deployed);
        player.containerMenu.broadcastChanges();

        if (deployed) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 2.0F);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WITCH,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        16, 0.45D, 0.75D, 0.45D, 0.02D);
            }
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!isDeployed(stack) || !(entity instanceof Player player)) {
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        if (!DeathsClothingItem.isFullSetWorn(player)) {
            setDeployed(stack, false);
            player.containerMenu.broadcastChanges();
            return;
        }

        int foodLevel = player.getFoodData().getFoodLevel();
        if (foodLevel > 0) {
            player.getFoodData().eat(foodLevel == 1 ? -1 : -2, 0.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.deaths_hand.tip").withStyle(ChatFormatting.BLUE));
    }

    public static boolean isDeployed(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBooleanOr(TAG_SCYTHE_DEPLOYED, false);
    }

    public static void setDeployed(ItemStack stack, boolean deployed) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (deployed) {
                tag.putBoolean(TAG_SCYTHE_DEPLOYED, true);
            } else {
                tag.remove(TAG_SCYTHE_DEPLOYED);
            }
        });
    }
}
