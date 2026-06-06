package com.emoniph.witchery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Source 1.7.10 Queasy: a harmful, incurable marker applied after strong Paralysed expires.
 */
public class QueasyMobEffect extends MobEffect {
    public static final int PARALYSED_EXPIRY_DURATION_TICKS = 1800;

    public QueasyMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
