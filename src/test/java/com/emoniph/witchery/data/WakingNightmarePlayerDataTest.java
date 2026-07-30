package com.emoniph.witchery.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WakingNightmarePlayerDataTest {
    @Test
    void curseLevelClampsAndReportsActiveState() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        assertFalse(data.hasWakingNightmare());

        data.setWakingNightmareLevel(3);
        assertEquals(3, data.getWakingNightmareLevel());
        assertTrue(data.hasWakingNightmare());

        data.setWakingNightmareLevel(-1);
        assertEquals(0, data.getWakingNightmareLevel());
        assertFalse(data.hasWakingNightmare());
    }

    @Test
    void curseStateCopiesWithPersistentPlayerData() {
        WitcheryPlayerData original = new WitcheryPlayerData();
        original.setWakingNightmareLevel(2);

        WitcheryPlayerData copy = new WitcheryPlayerData();
        copy.copyFrom(original, false);

        assertEquals(2, copy.getWakingNightmareLevel());
        assertTrue(copy.hasWakingNightmare());
    }
}
