package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of Revealing — night vision + glowing, reveals hidden things. */
public class BrewOfRevealingItem extends BrewItem {

    public BrewOfRevealingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0)); // 30s
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING,      600, 0)); // 30s
    }
}
