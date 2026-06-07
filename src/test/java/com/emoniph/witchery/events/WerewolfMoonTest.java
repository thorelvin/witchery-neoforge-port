package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the werewolf full-moon timing: full moon is moon phase 0 (every 8th day), and a "full-moon
 * night" additionally requires the time-of-day to fall in the night window (13000–23000). Pure
 * arithmetic over {@code dayTime}, no level needed.
 */
class WerewolfMoonTest {

    private static final long DAY = 24000L;

    @Test
    void fullMoonIsEveryEighthDay() {
        assertTrue(WerewolfEvents.isFullMoon(0L), "day 0 is a full moon");
        assertFalse(WerewolfEvents.isFullMoon(DAY), "day 1 is not");
        assertTrue(WerewolfEvents.isFullMoon(8 * DAY), "day 8 is a full moon again");
        assertFalse(WerewolfEvents.isFullMoon(3 * DAY), "day 3 is not");
        // Time-of-day within a full-moon day does not change the phase.
        assertTrue(WerewolfEvents.isFullMoon(15000L), "still day 0, still full moon");
    }

    @Test
    void fullMoonNightNeedsTheNightWindow() {
        assertTrue(WerewolfEvents.isFullMoonNight(15000L), "full-moon day, deep night");
        assertFalse(WerewolfEvents.isFullMoonNight(0L), "full-moon day but it is morning");
        assertFalse(WerewolfEvents.isFullMoonNight(13000L), "edge: not yet past dusk");
        assertFalse(WerewolfEvents.isFullMoonNight(23000L), "edge: already at dawn");
        assertFalse(WerewolfEvents.isFullMoonNight(DAY + 15000L), "night, but day 1 is not a full moon");
    }
}
