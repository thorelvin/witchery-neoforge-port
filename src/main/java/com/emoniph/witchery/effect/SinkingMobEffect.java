package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Sinking — source {@code PotionSinking} (1.7.10-0.24.1): an incurable curse that drags the
 * afflicted down through water and refuses to let them float.
 *
 * <p>Port divergence: the source applied its velocity nudges client-side for smoothness; this
 * port drives them from the server tick (velocity changes sync naturally), which keeps the
 * behaviour authoritative and works for both players and mobs. The downward bias and the
 * amplifier-scaled fall acceleration follow the source factors.</p>
 */
public class SinkingMobEffect extends MobEffect {

    public SinkingMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.isInWater()) {
            Vec3 v = entity.getDeltaMovement();
            double dy = v.y;
            double accel = Math.min(0.1D * (amplifier + 2), 0.4D);
            if (dy < 0.0D) {
                dy *= 1.0D + accel;            // sink faster
            } else {
                dy = -0.04D - 0.02D * amplifier; // never let them rise
            }
            entity.setDeltaMovement(v.x, dy, v.z);
            entity.hurtMarked = true; // force the new velocity to sync to the client
        }
        return true;
    }
}
