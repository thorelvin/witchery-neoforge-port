package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Brew of Flowing Spirit — the drinker briefly enters a semi-spectral state:
 * invisible, slow-falling, and glowing with spirit light.
 */
public class BrewOfFlowingSpiritItem extends BrewItem {

    public BrewOfFlowingSpiritItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,  400, 0)); // 20s
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,  400, 0)); // 20s
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,  400, 0)); // 20s
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING,       400, 0)); // 20s — visible to self
    }
}
