package com.emoniph.witchery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Feather Fall: clamp downward speed after enough fall distance,
 * then cap tracked fall distance by strength so landing damage is reduced.
 */
public class FeatherFallMobEffect extends MobEffect {
    public FeatherFallMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Vec3 motion = entity.getDeltaMovement();
        if (entity.fallDistance >= activationDistance(amplifier) && motion.y < -0.2D) {
            entity.setDeltaMovement(motion.x, -0.2D, motion.z);
            if (entity.fallDistance > maxFallDistance(amplifier)) {
                entity.fallDistance = maxFallDistance(amplifier);
            }
        }
        return true;
    }

    public static int activationDistance(int amplifier) {
        return amplifier >= 2 ? 3 : (amplifier >= 1 ? 4 : 5);
    }

    public static int maxFallDistance(int amplifier) {
        return amplifier >= 3 ? 3 : (amplifier >= 2 ? 4 : (amplifier >= 1 ? 5 : 6));
    }
}
