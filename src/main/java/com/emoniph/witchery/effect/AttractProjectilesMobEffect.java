package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Attract Projectiles: make nearby projectiles home in on the target.
 */
public class AttractProjectilesMobEffect extends MobEffect {
    public static final double FAST_PROJECTILE_SPEED_SQ = 0.25D;
    public static final double MIN_HORIZONTAL_DISTANCE = 1.0E-7D;

    public AttractProjectilesMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity target, int amplifier) {
        double radius = radius(amplifier);
        AABB bounds = target.getBoundingBox().inflate(radius, radius, radius);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, bounds)) {
            Vec3 motion = projectile.getDeltaMovement();
            double velocitySq = motion.lengthSqr();
            if (!canRedirect(projectile.tickCount, velocitySq)) {
                continue;
            }
            Vec3 vector = targetVector(target, projectile);
            if (horizontalDistance(vector) >= MIN_HORIZONTAL_DISTANCE) {
                projectile.shoot(vector.x, vector.y, vector.z, 1.0F, 1.0F);
                projectile.hurtMarked = true;
            }
        }
        return true;
    }

    public static double radius(int amplifier) {
        return (1.0D + amplifier) * 3.0D;
    }

    public static boolean canRedirect(int tickCount, double velocitySq) {
        return tickCount >= (velocitySq > FAST_PROJECTILE_SPEED_SQ ? 1 : 10);
    }

    public static Vec3 targetVector(LivingEntity target, Projectile projectile) {
        return new Vec3(
                target.getX() - projectile.getX(),
                target.getBoundingBox().minY + target.getBbHeight() * 0.75D - projectile.getY(),
                target.getZ() - projectile.getZ());
    }

    public static double horizontalDistance(Vec3 vector) {
        return Math.sqrt(vector.x * vector.x + vector.z * vector.z);
    }
}
