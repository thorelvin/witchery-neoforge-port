package com.emoniph.witchery.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;

/**
 * Base class for drinkable Witchery brews.
 * Subclasses override applyEffect() to do the brew-specific thing.
 * Consuming the item returns a glass bottle (like potions).
 */
public abstract class BrewItem extends Item {

    public BrewItem(Properties properties) {
        super(properties.stacksTo(3).craftRemainder(Items.GLASS_BOTTLE));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            applyEffect(player, level);
        }
        // Consume one brew, return a glass bottle
        stack.shrink(1);
        if (entity instanceof Player player && !player.isCreative()) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.DRINK; }

    @Override
    public net.minecraft.world.InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand);
    }

    protected abstract void applyEffect(Player player, Level level);
}
