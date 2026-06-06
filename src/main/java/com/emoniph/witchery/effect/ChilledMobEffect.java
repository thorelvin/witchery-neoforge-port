package com.emoniph.witchery.effect;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;

/**
 * Source 1.7.10 Chilled: a non-debuff cold marker that slows, dampens fire damage, and hurts Blazes.
 */
public class ChilledMobEffect extends MobEffect {
    public static final int BREW_DURATION_TICKS = 3600;
    public static final int FIRE_DAMAGE_REDUCTION_BASE = 1;
    public static final int HIGH_STRENGTH_COLD_DAMAGE_AMPLIFIER = 2;
    public static final float COLD_DAMAGE = 1.0F;
    private static final double SOURCE_SPEED_MODIFIER = -0.1D;
    private static final Identifier SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "effect.chilled");

    public ChilledMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, SOURCE_SPEED_MODIFIER,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = coldDamageTickInterval(amplifier);
        return interval <= 0 || duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (shouldTakeColdDamage(entity instanceof Blaze, amplifier)) {
            entity.hurtServer(level, level.damageSources().magic(), COLD_DAMAGE);
        }
        return true;
    }

    public static boolean isSourceFireDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    public static float fireDamageAfterReduction(float damage, int amplifier) {
        float reduced = damage - (float) (FIRE_DAMAGE_REDUCTION_BASE + amplifier);
        float floor = Math.min(damage, amplifier >= HIGH_STRENGTH_COLD_DAMAGE_AMPLIFIER ? 0.0F : 1.0F);
        return Math.max(reduced, floor);
    }

    public static int coldDamageTickInterval(int amplifier) {
        return 25 >> amplifier;
    }

    public static boolean shouldTakeColdDamage(boolean blaze, int amplifier) {
        return blaze || amplifier >= HIGH_STRENGTH_COLD_DAMAGE_AMPLIFIER;
    }
}
