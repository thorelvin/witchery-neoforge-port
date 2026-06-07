package com.emoniph.witchery.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the player-taglock capture odds the Book of Shadows quotes: 80% behind + crouching, 60%
 * behind + standing, 40% in front + crouching, 20% in front + standing. Pure arithmetic, no registry.
 */
class TaglockCaptureOddsTest {

    private static double successChance(boolean crouching, boolean behind) {
        return 1.0 - TaglockKitItem.playerTaglockFailChance(crouching, behind);
    }

    @Test
    void captureOddsMatchTheBook() {
        assertEquals(0.80, successChance(true, true), 1e-9, "behind + crouching");
        assertEquals(0.60, successChance(false, true), 1e-9, "behind + standing");
        assertEquals(0.40, successChance(true, false), 1e-9, "front + crouching");
        assertEquals(0.20, successChance(false, false), 1e-9, "front + standing");
    }
}
