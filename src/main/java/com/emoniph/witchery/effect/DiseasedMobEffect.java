package com.emoniph.witchery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Source 1.7.10 Diseased is harmful and incurable. Full liquid disease spread is
 * still its own brew-fluid slice; this effect exists so Purified Milk can restore
 * the old special cure branch cleanly.
 */
public class DiseasedMobEffect extends MobEffect {
    public static final int NORMAL_CURE_STRENGTH = 2;
    public static final int BLOCK_CLEANUP_STRENGTH = 3;
    public static final int BLOCK_CLEANUP_RADIUS = 9;

    public DiseasedMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
