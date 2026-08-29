package com.emoniph.witchery.spiritworld;

import net.minecraft.util.Mth;

/** Source-shaped Spirit World color and celestial-angle rules, isolated from client state for verification. */
public final class SpiritWorldAtmosphere {
    private static final float DREAM_ANGLE = 1.0F;
    private static final float NIGHTMARE_ANGLE = 0.5F;
    private static final long DREAM_TIME = 6_000L;
    private static final long NIGHTMARE_TIME = 18_000L;

    private SpiritWorldAtmosphere() {
    }

    public static Atmosphere forState(boolean nightmare, boolean demonic) {
        float red;
        float green;
        float blue;
        if (demonic) {
            red = 1.0F;
            green = 0.0F;
            blue = 0.0F;
        } else if (nightmare) {
            red = 0.0F;
            green = 1.0F;
            blue = 0.0F;
        } else {
            red = 0.8F;
            green = 0.2F;
            blue = 0.6F;
        }

        boolean nightmareLike = nightmare || demonic;
        float celestialAngle = nightmareLike ? NIGHTMARE_ANGLE : DREAM_ANGLE;
        float factor = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 2.0F + 0.5F,
                0.0F, 1.0F);
        Color color = new Color(
                red * (factor * 0.94F + 0.06F),
                green * (factor * 0.94F + 0.06F),
                blue * (factor * 0.91F + 0.09F));
        return new Atmosphere(color, celestialAngle * Mth.TWO_PI,
                nightmareLike ? NIGHTMARE_TIME : DREAM_TIME);
    }

    public record Atmosphere(Color color, float sunAngleRadians, long clientDayTime) {
    }

    public record Color(float red, float green, float blue) {
    }
}
