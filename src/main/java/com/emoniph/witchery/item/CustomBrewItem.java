package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The generic drinkable custom brew ("Brew" / brew_bottle). Its identity (name, colour, effects)
 * comes from the {@link BrewContents} composed in the Witch's Cauldron and stored on the stack.
 *
 * Source parity: Resurrected {@code ItemBrewBottle} (drink dispersal).
 */
public class CustomBrewItem extends Item {

    public CustomBrewItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    private static BrewContents contents(ItemStack stack) {
        return stack.getOrDefault(WitcheryDataComponents.BREW_CONTENTS.get(), BrewContents.EMPTY);
    }

    @Override
    public Component getName(ItemStack stack) {
        BrewContents brew = contents(stack);
        return brew.isEmpty() ? super.getName(stack) : brew.displayName();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return contents(stack).drinkDuration();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            contents(stack).applyToEntity(entity);
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        if (entity instanceof Player player) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        BrewContents brew = contents(stack);
        if (brew.isEmpty()) {
            return;
        }
        int drinkDuration = brew.drinkDuration();
        if (drinkDuration != 32) {
            tooltip.accept(Component.translatable("witchery.brew.drink_speed",
                    Component.translatable(drinkSpeedKey(drinkDuration))).withStyle(ChatFormatting.GRAY));
        }
        for (var effect : com.emoniph.witchery.brewing.Brews.buildEffects(brew.items())) {
            Component name = Component.translatable(effect.getEffect().value().getDescriptionId());
            if (effect.getAmplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            name = Component.translatable("potion.withDuration", name, formatDuration(effect.getDuration()));
            tooltip.accept(name.copy().withStyle(ChatFormatting.BLUE));
        }
        for (var action : com.emoniph.witchery.brewing.Brews.buildActions(brew.items())) {
            Component name = Component.translatable(action.nameKey());
            if (action.strength() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + action.strength()));
            }
            tooltip.accept(name.copy().withStyle(ChatFormatting.BLUE));
        }
    }

    private static Component formatDuration(int ticks) {
        int seconds = ticks / 20;
        return Component.literal(String.format("%d:%02d", seconds / 60, seconds % 60));
    }

    private static String drinkSpeedKey(int drinkDuration) {
        if (drinkDuration > 48) {
            return "witchery.brew.drink_speed.very_slow";
        }
        if (drinkDuration > 32) {
            return "witchery.brew.drink_speed.slow";
        }
        if (drinkDuration < 16) {
            return "witchery.brew.drink_speed.very_fast";
        }
        return "witchery.brew.drink_speed.fast";
    }
}
