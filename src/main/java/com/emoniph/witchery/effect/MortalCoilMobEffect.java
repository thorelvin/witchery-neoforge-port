package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Mortal Coil (Death Coil) — source {@code PotionMortalCoil} (1.7.10-0.24.1).
 *
 * <p>An incurable hex with no side effects while active: the moment it expires, the victim
 * dies. The Shade of Leonard lays it on every nearby player; the only escape is to defeat the
 * Shade, whose death clears the coil from everyone in range (see
 * {@link com.emoniph.witchery.entity.LeonardEntity}).</p>
 *
 * <p>Incurability is enforced at application time by clearing the instance's NeoForge cure set,
 * so milk will not wash it away.</p>
 */
public class MortalCoilMobEffect extends MobEffect {

    public MortalCoilMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Source func_76397_a: fire on the final tick (duration == 1).
        return duration <= 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Source func_76394_a -> EntityUtil.instantDeath.
        entity.invulnerableTime = 0;
        entity.hurtServer(level, level.damageSources().magic(), Float.MAX_VALUE);
        return true;
    }
}
