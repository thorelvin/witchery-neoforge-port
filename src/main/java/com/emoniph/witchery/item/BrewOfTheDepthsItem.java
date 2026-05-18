package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of the Depths — extended water breathing, dolphin's grace, and night vision. */
public class BrewOfTheDepthsItem extends BrewItem {
    public BrewOfTheDepthsItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 6000, 0)); // 5 min
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,  2400, 0)); // 2 min
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,    2400, 0)); // 2 min
    }
}
