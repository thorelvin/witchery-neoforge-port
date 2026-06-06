package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The generic throwable custom brew — {@code splash_brew_bottle} (instant area) or
 * {@code lingering_brew_bottle} (gas cloud). Its effects come from the composed {@link BrewContents}.
 *
 * Source parity: Resurrected {@code ItemBrewBottle} splash/lingering dispersals.
 */
public class CustomThrownBrewItem extends Item {

    private final boolean gas;

    public CustomThrownBrewItem(Properties properties, boolean gas) {
        super(properties.stacksTo(16));
        this.gas = gas;
    }

    boolean isGas() {
        return gas;
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
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            ThrownBrewEntity projectile = new ThrownBrewEntity(level, player, contents(stack), gas);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(projectile);
            if (!player.isCreative()) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        BrewContents brew = contents(stack);
        if (brew.isEmpty()) {
            return;
        }
        tooltip.accept(Component.translatable(gas ? "witchery.brew.dispersal.gas" : "witchery.brew.dispersal.splash")
                .withStyle(ChatFormatting.GRAY));
        for (var effect : com.emoniph.witchery.brewing.Brews.buildEffects(brew.items())) {
            Component name = Component.translatable(effect.getEffect().value().getDescriptionId());
            if (effect.getAmplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + effect.getAmplifier()));
            }
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
}
