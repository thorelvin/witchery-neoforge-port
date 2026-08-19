package com.emoniph.witchery.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpiritWorldPlacementTest {
    @Test
    void sourceVerticalSeedNeverExtendsBelowOldWorldFloor() {
        assertEquals(0, SpiritWorldPlacement.sourceMinY(-64));
        assertEquals(0, SpiritWorldPlacement.sourceMinY(0));
        assertEquals(32, SpiritWorldPlacement.sourceMinY(32));
    }

    @Test
    void sourceVerticalSeedEndsThirtyTwoBlocksAboveSurfaceWithinBuildHeight() {
        assertEquals(102, SpiritWorldPlacement.sourceMaxYExclusive(-64, 384, 70));
        assertEquals(320, SpiritWorldPlacement.sourceMaxYExclusive(-64, 384, 400));
    }
}
