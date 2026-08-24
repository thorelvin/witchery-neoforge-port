package com.emoniph.witchery.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerBloodTest {
    @Test
    void awakeVictimsYieldTwoThirdsButKnockedOutVictimsYieldTheFullAmount() {
        assertEquals(7, VillagerBlood.sourceAdjustedDrain(10, false));
        assertEquals(20, VillagerBlood.sourceAdjustedDrain(30, false));
        assertEquals(10, VillagerBlood.sourceAdjustedDrain(10, true));
        assertEquals(0, VillagerBlood.sourceAdjustedDrain(-10, false));
    }

    @Test
    void sleepRecoveryMatchesTheSourceMinuteThresholds() {
        assertEquals(0, VillagerBlood.sourceSleepRecovery(20 * 60));
        assertEquals(50, VillagerBlood.sourceSleepRecovery(20 * 60 + 1));
        assertEquals(100, VillagerBlood.sourceSleepRecovery(20 * 60 * 2));
        assertFalse(VillagerBlood.sourceSleepFullyHeals(20 * 60 * 2 - 1));
        assertTrue(VillagerBlood.sourceSleepFullyHeals(20 * 60 * 2));
    }
}
