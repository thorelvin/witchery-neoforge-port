package com.emoniph.witchery.spiritworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpiritWorldAtmosphereTest {
    @Test
    void dreamNightmareAndDemonicStatesKeepDistinctPalettes() {
        SpiritWorldAtmosphere.Atmosphere dream = SpiritWorldAtmosphere.forState(false, false);
        SpiritWorldAtmosphere.Atmosphere nightmare = SpiritWorldAtmosphere.forState(true, false);
        SpiritWorldAtmosphere.Atmosphere demonic = SpiritWorldAtmosphere.forState(true, true);

        assertTrue(dream.color().red() > dream.color().green());
        assertTrue(dream.color().blue() > dream.color().green());
        assertEquals(0.0F, nightmare.color().red());
        assertTrue(nightmare.color().green() > 0.0F);
        assertEquals(0.0F, nightmare.color().blue());
        assertTrue(demonic.color().red() > 0.0F);
        assertEquals(0.0F, demonic.color().green());
        assertEquals(0.0F, demonic.color().blue());
    }

    @Test
    void ordinaryAndDemonicNightmaresShareTheSourceSunAngle() {
        float dream = SpiritWorldAtmosphere.forState(false, false).sunAngleRadians();
        float nightmare = SpiritWorldAtmosphere.forState(true, false).sunAngleRadians();
        float demonic = SpiritWorldAtmosphere.forState(false, true).sunAngleRadians();

        assertTrue(dream > nightmare);
        assertEquals(nightmare, demonic);
    }

    @Test
    void clientClockMatchesTheSourceProviderWithoutChangingServerTime() {
        assertEquals(6_000L, SpiritWorldAtmosphere.forState(false, false).clientDayTime());
        assertEquals(18_000L, SpiritWorldAtmosphere.forState(true, false).clientDayTime());
        assertEquals(18_000L, SpiritWorldAtmosphere.forState(true, true).clientDayTime());
    }
}
