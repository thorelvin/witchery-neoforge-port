package com.emoniph.witchery.spiritworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightmarePressureTest {
    @Test
    void wakingNightmareRollsOncePerSecond() {
        assertTrue(SpiritWorldHelper.shouldTickWakingNightmare(0));
        assertFalse(SpiritWorldHelper.shouldTickWakingNightmare(1));
        assertFalse(SpiritWorldHelper.shouldTickWakingNightmare(19));
        assertTrue(SpiritWorldHelper.shouldTickWakingNightmare(20));
        assertTrue(SpiritWorldHelper.shouldTickWakingNightmare(100));
    }

    @Test
    void demonicNightmareCatalystsKeepSourceCaps() {
        assertEquals(0.0D, SpiritWorldHelper.sourceDemonicChance(0, 0));
        assertEquals(0.45D, SpiritWorldHelper.sourceDemonicChance(1, 1), 0.0001D);
        assertEquals(1.0D, SpiritWorldHelper.sourceDemonicChance(2, 3), 0.0001D);
        assertEquals(1.0D, SpiritWorldHelper.sourceDemonicChance(20, 30), 0.0001D);
    }
}
