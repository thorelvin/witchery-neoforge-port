package com.emoniph.witchery.effect;

import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Source 1.7.10 Ender Inhibition: affected living entities cannot make ender teleports.
 */
public class EnderInhibitionMobEffect extends MobEffect {
    public EnderInhibitionMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    public static boolean canTeleport(Entity entity, int requiredAmplifier) {
        if (!(entity instanceof LivingEntity living)) {
            return true;
        }
        MobEffectInstance effect = living.getEffect(WitcheryMobEffects.ENDER_INHIBITION);
        return effect == null || canTeleport(effect.getAmplifier(), requiredAmplifier);
    }

    public static boolean canTeleport(int activeAmplifier, int requiredAmplifier) {
        return activeAmplifier < requiredAmplifier;
    }
}
