package com.emoniph.witchery.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DemonDropParityTest {
    @Test
    void magmaCreamRollUsesSourceExclusiveBound() {
        assertEquals(2, WitcheryMobDropEvents.sourceDemonMagmaCreamDropBound(0));
        assertEquals(3, WitcheryMobDropEvents.sourceDemonMagmaCreamDropBound(1));
        assertEquals(5, WitcheryMobDropEvents.sourceDemonMagmaCreamDropBound(3));
        assertEquals(2, WitcheryMobDropEvents.sourceDemonMagmaCreamDropBound(-1));
    }

    @Test
    void arthanaHeartChanceScalesEveryTwoLootingLevels() {
        assertEquals(0.33D, WitcheryMobDropEvents.sourceDemonHeartChance(0), 0.000001D);
        assertEquals(0.33D, WitcheryMobDropEvents.sourceDemonHeartChance(1), 0.000001D);
        assertEquals(0.66D, WitcheryMobDropEvents.sourceDemonHeartChance(2), 0.000001D);
        assertEquals(0.66D, WitcheryMobDropEvents.sourceDemonHeartChance(3), 0.000001D);
        assertEquals(0.99D, WitcheryMobDropEvents.sourceDemonHeartChance(4), 0.000001D);
        assertEquals(1.0D, WitcheryMobDropEvents.sourceDemonHeartChance(6), 0.000001D);
    }
}
