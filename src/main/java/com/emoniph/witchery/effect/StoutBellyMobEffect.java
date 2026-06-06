package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Source 1.7.10 Stout Belly: amplified levels clear Hunger on a one-second tick.
 */
public class StoutBellyMobEffect extends MobEffect {
    public StoutBellyMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (shouldRemoveHunger(level.getGameTime(), amplifier, entity.hasEffect(MobEffects.HUNGER))) {
            entity.removeEffect(MobEffects.HUNGER);
        }
        return true;
    }

    public static boolean shouldRemoveHunger(long gameTime, int amplifier, boolean hasHunger) {
        return gameTime % 20L == 3L && amplifier > 0 && hasHunger;
    }
}
