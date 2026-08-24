package com.emoniph.witchery.effect;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Source resizing tiers expressed through modern entity scale attributes. */
public final class ResizingMobEffect extends MobEffect {
    private static final Identifier SCALE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "effect.resizing");

    public ResizingMobEffect(int color) {
        super(MobEffectCategory.NEUTRAL, color);
    }

    public static float sourceScaleFactor(int amplifier) {
        return switch (amplifier) {
            case 1 -> 0.4F;
            case 2 -> 2.0F;
            case 3 -> 3.0F;
            default -> 0.25F;
        };
    }

    public static void syncScale(LivingEntity entity, int amplifier) {
        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.addOrUpdateTransientModifier(new AttributeModifier(SCALE_MODIFIER_ID,
                    sourceScaleFactor(amplifier) - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void removeScale(LivingEntity entity) {
        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.removeModifier(SCALE_MODIFIER_ID);
        }
    }

    public static float sourceMeleeDamageMultiplier(MobEffectInstance attacker, MobEffectInstance defender) {
        return sourceMeleeDamageMultiplierForTiers(sourceSizeTier(attacker), sourceSizeTier(defender));
    }

    static float sourceMeleeDamageMultiplierForTiers(int attackerSize, int defenderSize) {
        float sourceRatio = (float) attackerSize / Math.max(1, defenderSize);
        return Math.max(0.5F, Math.min(3.0F, sourceRatio));
    }

    public static double sourceJumpMultiplier(int amplifier) {
        float scale = sourceScaleFactor(amplifier);
        return scale > 1.0F
                ? scale * 0.5D + 0.5D
                : Math.max(scale, 0.5D) * 1.5D;
    }

    private static int sourceSizeTier(MobEffectInstance effect) {
        if (effect == null) {
            return 3;
        }
        return switch (effect.getAmplifier()) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 5;
            default -> 3;
        };
    }
}
