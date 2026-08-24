package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Source {@code PotionDarknessAllergy}: darkness hurts at an amplifier-scaled light threshold. */
public final class DarknessAllergyMobEffect extends MobEffect {
    public DarknessAllergyMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 4;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (level.getMaxLocalRawBrightness(entity.blockPosition()) < 2 + amplifier * 2) {
            entity.hurtServer(level, level.damageSources().magic(), 1.0F);
        }
        return true;
    }
}
