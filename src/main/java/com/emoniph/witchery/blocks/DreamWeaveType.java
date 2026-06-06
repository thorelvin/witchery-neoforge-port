package com.emoniph.witchery.blocks;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Five dream weave types, carrying their original 1.7.10 potion data. */
public enum DreamWeaveType {
    //                id  dream effect                 nightmare effect               dur   amp
    FLEET_FOOT   (0, MobEffects.SPEED,       MobEffects.SLOWNESS, 7200, 0),
    IRON_ARM     (1, MobEffects.HASTE,            MobEffects.MINING_FATIGUE,      7200, 0),
    FASTING      (2, MobEffects.SATURATION,           MobEffects.POISON,            4800, 0),
    NIGHTMARES   (3, MobEffects.WEAKNESS,             MobEffects.BLINDNESS,         1200, 0),
    INTENSITY    (4, MobEffects.NIGHT_VISION,         MobEffects.BLINDNESS,          300, 0);

    public final int id;

    private final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> dream;
    private final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> nightmare;
    private final int duration;
    private final int amplifier;

    DreamWeaveType(int id,
                   net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> dream,
                   net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> nightmare,
                   int duration, int amplifier) {
        this.id        = id;
        this.dream     = dream;
        this.nightmare = nightmare;
        this.duration  = duration;
        this.amplifier = amplifier;
    }

    /**
     * Apply dream/nightmare effect to a player.
     * isDream   = no NIGHTMARES catcher in range.
     * isEnhanced = INTENSITY catcher in range.
     *
     * Mirrors ItemGeneral.DreamWeave.applyEffect logic from 1.7.10:
     *  - Dream + enhanced + Saturation → longer duration (+2400 t)
     *  - Dream + enhanced + other      → shorter duration (-2400 t) but +1 amplifier
     *  - Nightmare                     → nightmare potion, amplifier +1 if enhanced
     */
    public void applyEffect(Player player, boolean isDream, boolean isEnhanced) {
        if (isDream) {
            boolean isSaturation = dream == MobEffects.SATURATION;
            int d = isEnhanced ? (isSaturation ? duration + 2400 : duration - 2400)
                               : duration;
            int a = (isEnhanced && !isSaturation) ? amplifier + 1 : amplifier;
            player.addEffect(new MobEffectInstance(dream, d, a));
        } else {
            player.addEffect(new MobEffectInstance(nightmare, duration,
                    isEnhanced ? amplifier + 1 : amplifier));
        }
    }

    public @Nullable static DreamWeaveType fromId(int id) {
        for (DreamWeaveType t : values()) if (t.id == id) return t;
        return null;
    }
}
