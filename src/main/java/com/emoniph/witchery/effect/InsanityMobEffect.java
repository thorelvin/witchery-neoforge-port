package com.emoniph.witchery.effect;

import com.emoniph.witchery.entity.IllusionEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Insanity (Madness) - source PotionInsanity: victim-only phantom monsters and unnerving sounds.
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
        if (level.random.nextInt(hallucinationChance(strength)) == 0) {
            if (!IllusionEntity.spawnRandomNear(level, player, strength) && strength >= 4) {
                playHighLevelSound(level, player);
            }
        } else if (strength >= 4 && level.random.nextInt(20) == 0) {
            playHighLevelSound(level, player);
        }
        return true;
    }

    public static int hallucinationChance(int strength) {
        return strength > 2 ? 25 : (strength > 1 ? 30 : 35);
    }

    private static void playHighLevelSound(ServerLevel level, ServerPlayer player) {
        SoundEvent sound = level.random.nextInt(3) == 1
                ? SoundEvents.ENDERMAN_AMBIENT
                : SoundEvents.GENERIC_EXPLODE.value();
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        player.connection.send(new ClientboundSoundPacket(holder, SoundSource.HOSTILE,
                player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F, level.random.nextLong()));
    }
}
