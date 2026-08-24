package com.emoniph.witchery.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

/** Source {@code PotionLove}: repeatedly puts adult animals into breeding mode. */
public final class LoveMobEffect extends MobEffect {
    public LoveMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 7;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Animal animal && animal.getAge() >= 0 && !animal.isInLove()) {
            animal.setAge(0);
            animal.setInLove(null);
        } else if (level.random.nextBoolean()) {
            level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY(0.7D), entity.getZ(),
                    2, 0.3D, 0.3D, 0.3D, 0.0D);
        }
        return true;
    }
}
