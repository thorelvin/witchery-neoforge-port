package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Brew of the Grotesque — monstrous transformation: extreme strength and resilience
 * at the cost of speed, control, and appearance. 30 seconds.
 */
public class BrewOfTheGrotesqueItem extends BrewItem {

    public BrewOfTheGrotesqueItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,     600, 3)); // 30s Strength IV
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 1)); // 30s Resistance II
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 3)); // 30s Slowness IV
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,         600, 0)); // 30s Nausea
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING,           600, 0)); // 30s — grotesque glow
    }
}
