package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Source {@code PotionSpiked}: nearby creatures take cactus damage from the afflicted entity. */
public final class SpikedMobEffect extends MobEffect {
    public SpikedMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 3;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        double radius = 0.2D + 0.1D * amplifier;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(radius, 0.0D, radius), other -> other != entity)) {
            other.hurtServer(level, level.damageSources().cactus(), 1.0F + amplifier);
        }
        return true;
    }
}
