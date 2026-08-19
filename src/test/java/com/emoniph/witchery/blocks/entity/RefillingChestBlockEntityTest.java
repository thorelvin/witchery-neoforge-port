package com.emoniph.witchery.blocks.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefillingChestBlockEntityTest {
    @Test
    void sourceRefillClockRunsOnlyForEmptyTormentChests() {
        assertTrue(RefillingChestBlockEntity.shouldRefill(true, true, 0L));
        assertTrue(RefillingChestBlockEntity.shouldRefill(
                true, true, RefillingChestBlockEntity.REFILL_TICKS));
        assertFalse(RefillingChestBlockEntity.shouldRefill(true, true, 1L));
        assertFalse(RefillingChestBlockEntity.shouldRefill(true, false, 0L));
        assertFalse(RefillingChestBlockEntity.shouldRefill(false, true, 0L));
    }
}
