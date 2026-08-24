package com.emoniph.witchery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Source Colorful curse, including the legacy Witchery dye ordinal palette. */
public final class ColorfulMobEffect extends MobEffect {
    private static final int[] SOURCE_DYE_RGB = {
            1118481, 12464176, 5732898, 5057301,
            2247599, 8532146, 3968688, 11513789,
            7763574, 15574987, 8639516, 15197994,
            12179199, 14383829, 15113780, 16777215
    };

    public ColorfulMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    public static int sourceTint(int amplifier) {
        return SOURCE_DYE_RGB[Math.clamp(amplifier, 0, SOURCE_DYE_RGB.length - 1)];
    }
}
