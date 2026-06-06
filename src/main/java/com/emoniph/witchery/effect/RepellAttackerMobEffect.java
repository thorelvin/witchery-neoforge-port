package com.emoniph.witchery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Repell Attacker strength scaling.
 * The actual shove happens from the damage event so projectile hits can be ignored.
 */
public class RepellAttackerMobEffect extends MobEffect {
    public static final double MAX_RANGE_SQ = 9.0D;

    public RepellAttackerMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    public static double horizontalStrength(int amplifier) {
        return 1.0D + (double) amplifier * 0.75D;
    }

    public static double verticalStrength(int amplifier) {
        return 0.5D + (double) amplifier * 0.2D;
    }

    public static Vec3 pushbackMotion(Entity attacker, Entity protectedEntity, int amplifier) {
        Vec3 direction = protectedEntity.position().subtract(attacker.position()).normalize();
        double horizontal = horizontalStrength(amplifier);
        double vertical = verticalStrength(amplifier);
        return new Vec3(-direction.x * horizontal, Math.max(-direction.y, vertical), -direction.z * horizontal);
    }
}
