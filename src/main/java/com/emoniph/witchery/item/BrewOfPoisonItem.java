package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of Poison — applies Poison III to the drinker (use with care). */
public class BrewOfPoisonItem extends BrewItem {

    public BrewOfPoisonItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 2)); // 15s Poison III
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1)); // 10s Weakness II
    }
}
