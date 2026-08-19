package com.emoniph.witchery.world;

/** Source-derived client atmosphere constants for the fixed Torment sky. */
public final class TormentAtmosphere {
    public static final int SKY_RGB = 0xFF0000;
    public static final int FOG_RGB = 0xA03232;
    public static final int PORTAL_RGB = 0xFF0022;
    public static final float FIXED_SUN_ANGLE_RADIANS = (float) (Math.PI * 2.0D);

    private TormentAtmosphere() {}

    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }
}
