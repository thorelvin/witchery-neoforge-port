package com.emoniph.witchery.blocks.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class PlacedModonomiconBlockEntityTest {
    @Test
    void nextOpenPagesIndexMovesByDoublePageSpreads() {
        assertEquals(2, PlacedModonomiconBlockEntity.nextOpenPagesIndex(0, 6, 1));
        assertEquals(4, PlacedModonomiconBlockEntity.nextOpenPagesIndex(2, 6, 1));
        assertEquals(2, PlacedModonomiconBlockEntity.nextOpenPagesIndex(4, 6, -1));
    }

    @Test
    void nextOpenPagesIndexClampsToExistingEvenSpread() {
        assertEquals(4, PlacedModonomiconBlockEntity.nextOpenPagesIndex(4, 5, 1));
        assertEquals(4, PlacedModonomiconBlockEntity.nextOpenPagesIndex(6, 5, 1));
        assertEquals(0, PlacedModonomiconBlockEntity.nextOpenPagesIndex(0, 5, -1));
    }

    @Test
    void nextOpenPagesIndexSanitizesOddAndEmptyInputs() {
        assertEquals(4, PlacedModonomiconBlockEntity.nextOpenPagesIndex(3, 7, 1));
        assertEquals(0, PlacedModonomiconBlockEntity.nextOpenPagesIndex(3, 0, 1));
        assertEquals(2, PlacedModonomiconBlockEntity.nextOpenPagesIndex(3, 7, 0));
    }

    @Test
    void nearbyReaderDoesNotRequireLookingAtBook() {
        assertTrue(PlacedModonomiconBlockEntity.isNearbyReader(
                BlockPos.ZERO,
                0.5D,
                1.62D,
                3.5D));
    }

    @Test
    void nearbyReaderRejectsOutOfRangeAndBelowBook() {
        assertFalse(PlacedModonomiconBlockEntity.isNearbyReader(
                BlockPos.ZERO,
                0.5D,
                1.62D,
                7.0D));
        assertFalse(PlacedModonomiconBlockEntity.isNearbyReader(
                BlockPos.ZERO,
                0.5D,
                -0.1D,
                0.5D));
    }
}
