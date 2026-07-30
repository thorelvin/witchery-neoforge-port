package com.emoniph.witchery.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrotesquePlayerDataTest {

    @Test
    void fixedBrewTimerClampsAndExpires() {
        WitcheryPlayerData data = new WitcheryPlayerData();

        data.setGrotesqueTicks(-1);
        assertEquals(0, data.getGrotesqueTicks());
        assertFalse(data.hasGrotesqueAura());

        data.setGrotesqueTicks(2);
        assertTrue(data.hasGrotesqueAura());
        data.tickGrotesque();
        assertEquals(1, data.getGrotesqueTicks());
        assertTrue(data.hasGrotesqueAura());
        data.tickGrotesque();
        assertEquals(0, data.getGrotesqueTicks());
        assertFalse(data.hasGrotesqueAura());
        data.tickGrotesque();
        assertEquals(0, data.getGrotesqueTicks());
    }
}
