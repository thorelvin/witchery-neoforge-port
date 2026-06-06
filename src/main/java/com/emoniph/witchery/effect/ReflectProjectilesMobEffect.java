package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Reflect Projectiles: reverse nearby incoming projectile motion.
 */
public class ReflectProjectilesMobEffect extends MobEffect {
    public static final double RADIUS = 2.0D;

    public ReflectProjectilesMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        AABB bounds = entity.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, bounds)) {
            if (projectile.getOwner() == entity) {
                continue;
            }
            projectile.setDeltaMovement(reflectedMotion(projectile.getDeltaMovement(), amplifier));
            projectile.hurtMarked = true;
        }
        return true;
    }

    public static Vec3 reflectedMotion(Vec3 motion, int amplifier) {
        double factor = reflectFactor(amplifier);
        double x = motion.x * factor;
        double y = motion.y;
        if (x > 0.0D || motion.z > 0.0D) {
            y = motion.y * factor;
        }
        double z = motion.z * factor;
        return new Vec3(x, y, z);
    }

    public static double reflectFactor(int amplifier) {
        return -0.25D * (1.0D + amplifier);
    }
}
