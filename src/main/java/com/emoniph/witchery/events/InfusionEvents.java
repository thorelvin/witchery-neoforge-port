package com.emoniph.witchery.events;

import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Applies passive effects to players based on their active InfusionType.
 * Effects are re-applied every 100 ticks so they stay active without
 * using excessively long amplifier durations.
 */
@EventBusSubscriber
public class InfusionEvents {

    private static final int REFRESH_INTERVAL = 100; // ticks between effect refreshes
    private static final int EFFECT_DURATION   = 140; // ticks — slightly longer than interval

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % REFRESH_INTERVAL != 0) return;

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        InfusionType type = data.getInfusionType();
        if (type == InfusionType.NONE) return;

        switch (type) {
            case LIGHT      -> applyLight(player);
            case OVERWORLD  -> applyOverworld(player);
            case OTHERWHERE -> applyOtherwhere(player);
            case INFERNAL   -> applyInfernal(player);
        }
    }

    // ── Passive effect sets ───────────────────────────────────────────────────

    /** Infusion of Light — Glowing + Night Vision + minor day resistance (Absorption I). */
    private static void applyLight(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING,      EFFECT_DURATION, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,   EFFECT_DURATION, 0, true, false));
    }

    /** Infusion of Overworld — Speed I + Haste I when outdoors during daylight. */
    private static void applyOverworld(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, 0, true, false));
        if (player.level().getDayTime() % 24000 < 12000) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, EFFECT_DURATION, 0, true, false));
        }
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION,   EFFECT_DURATION, 0, true, false));
    }

    /** Infusion of Otherwhere — Slow Fall + Speed II + Enderman-like agility. */
    private static void applyOtherwhere(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_DURATION, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, 1, true, false)); // Speed II
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, EFFECT_DURATION, 1, true, false));           // Jump Boost II
    }

    /** Infernal Infusion — Fire Resistance + Strength I + Darkness (for theme). */
    private static void applyInfernal(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,    EFFECT_DURATION, 0, true, false)); // Strength I
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,        EFFECT_DURATION, 0, true, false));
    }
}
