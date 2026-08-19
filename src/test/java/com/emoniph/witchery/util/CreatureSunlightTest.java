package com.emoniph.witchery.util;

import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureSunlightTest {
    @Test
    void sourceExcludesSpiritTormentAndSkylessDimensions() {
        assertFalse(CreatureSunlight.dimensionAllowsSunlight(WitcheryDimensions.SPIRIT_WORLD, true));
        assertFalse(CreatureSunlight.dimensionAllowsSunlight(WitcheryDimensions.TORMENT, true));
        assertFalse(CreatureSunlight.dimensionAllowsSunlight(Level.NETHER, false));
        assertTrue(CreatureSunlight.dimensionAllowsSunlight(Level.OVERWORLD, true));
    }
}
