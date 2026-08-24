package com.emoniph.witchery.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Source {@code PotionHellishAura}: scorches creatures crowding the afflicted entity. */
public final class HellishAuraMobEffect extends MobEffect {
    public HellishAuraMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> Math.min(amplifier, 4);
        return interval <= 1 || duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(1.5D, 0.0D, 1.5D), other -> other != entity)) {
            other.hurtServer(level, level.damageSources().indirectMagic(entity, entity), 1.0F);
            if (amplifier > 0) {
                other.igniteForSeconds(amplifier);
            }
            level.sendParticles(ParticleTypes.FLAME, other.getX(), other.getY(0.5D), other.getZ(),
                    8, 0.25D, 0.4D, 0.25D, 0.02D);
        }
        return true;
    }
}
