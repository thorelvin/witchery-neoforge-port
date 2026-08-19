package com.emoniph.witchery.util;

import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Shared source dimension gate for creature sunlight checks. */
public final class CreatureSunlight {
    private CreatureSunlight() {
    }

    public static boolean dimensionAllowsSunlight(Level level) {
        return dimensionAllowsSunlight(level.dimension(), level.dimensionType().hasSkyLight());
    }

    static boolean dimensionAllowsSunlight(ResourceKey<Level> dimension, boolean hasSkyLight) {
        return hasSkyLight
                && !dimension.equals(WitcheryDimensions.SPIRIT_WORLD)
                && !dimension.equals(WitcheryDimensions.TORMENT);
    }
}
