package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Throwable custom brew item (splash or gas dispersal tier).
 * Right-clicking throws a ThrownBrewEntity that applies the effect at the landing point.
 */
public class ThrownBrewItem extends Item {

    private final BrewType brewType;
    private final boolean gas;

    public ThrownBrewItem(Properties props, BrewType brewType, boolean gas) {
        super(props.stacksTo(3).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
        this.brewType = brewType;
        this.gas = gas;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide()) {
            int tier = stack.getOrDefault(WitcheryDataComponents.BREW_POWER.get(), 1);
            ThrownBrewEntity projectile = new ThrownBrewEntity(level, player, brewType, gas, tier);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0f, 0.5f, 1.0f);
            level.addFreshEntity(projectile);
            if (!player.isCreative()) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
