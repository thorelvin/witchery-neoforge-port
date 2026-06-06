package com.emoniph.witchery.effect;

import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Source 1.7.10 Feel No Pain / Moonshine: convert player hurt into hunger.
 */
public class MoonshineMobEffect extends MobEffect {
    public MoonshineMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (shouldRollHunger(level.getGameTime(), amplifier, entity.hasEffect(MobEffects.HUNGER),
                entity.hasEffect(WitcheryMobEffects.STOUT_BELLY))
                && level.random.nextInt(hungerRollBound(amplifier)) == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, hungerDurationTicks(amplifier)));
        }
        return true;
    }

    public static boolean shouldRollHunger(long gameTime, int amplifier, boolean hasHunger, boolean hasStoutBelly) {
        return gameTime % 20L == 2L && amplifier > 0 && !hasHunger && !hasStoutBelly;
    }

    public static int hungerDurationTicks(int amplifier) {
        return (6 + amplifier * 2) * 20;
    }

    public static int hungerRollBound(int amplifier) {
        return 5 - Math.min(amplifier, 3);
    }

    public static boolean isSourcePainDamage(DamageSource source) {
        return source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.PLAYER_ATTACK)
                || source.getEntity() instanceof LivingEntity;
    }

    public static int foodPenalty(float damage, int amplifier, int food) {
        if (food <= 0) {
            return 0;
        }
        double modifiedDamage = amplifier > 0
                ? Math.max(damage / (float) amplifier, amplifier > 1 ? 1.0F : 2.0F)
                : Math.max(damage * 2.0F, 3.0F);
        return Math.min((int) Math.ceil(modifiedDamage), food);
    }
}
