package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Insanity (Madness) — source {@code PotionInsanity} (1.7.10-0.24.1): an incurable curse that
 * preys on the mind, conjuring phantom monsters and unnerving sounds.
 *
 * <p><b>Port divergence:</b> the source spawned harmless <em>illusion</em> creepers/spiders/
 * zombies that vanish on contact. Those illusion entities are not yet ported, so this slice
 * approximates the madness with rolling Nausea plus the source's randomised eerie/explosion
 * cues played only to the victim. Wiring in true illusion mobs is a deferred follow-up; the
 * tick cadence, level-scaled odds, and high-level sound roll mirror the source.</p>
 */
public class InsanityMobEffect extends MobEffect {

    public InsanityMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 13;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        int strength = Math.max(0, amplifier) + 1;
        // Stand-in for the illusion-mob conjuring: a creeping nausea, more frequent at higher levels.
        int hallucinationChance = strength > 2 ? 25 : (strength > 1 ? 30 : 35);
        if (level.random.nextInt(hallucinationChance) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 90, 0, false, false));
        } else if (strength >= 4 && level.random.nextInt(20) == 0) {
            SoundEvent sound = level.random.nextInt(3) == 1
                    ? SoundEvents.ENDERMAN_AMBIENT
                    : SoundEvents.GENERIC_EXPLODE.value();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return true;
    }
}
