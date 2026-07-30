package com.emoniph.witchery.blocks.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SilverVatBlockEntityTest {
    @Test
    void visibleDepositPartsFollowSourceEightItemThresholds() {
        assertEquals(0, SilverVatBlockEntity.visibleDepositParts(0));
        assertEquals(1, SilverVatBlockEntity.visibleDepositParts(1));
        assertEquals(1, SilverVatBlockEntity.visibleDepositParts(8));
        assertEquals(1, SilverVatBlockEntity.visibleDepositParts(15));
        assertEquals(2, SilverVatBlockEntity.visibleDepositParts(16));
        assertEquals(7, SilverVatBlockEntity.visibleDepositParts(63));
        assertEquals(8, SilverVatBlockEntity.visibleDepositParts(64));
    }

    @Test
    void visibleDepositPartsClampInvalidOrOversizedCounts() {
        assertEquals(0, SilverVatBlockEntity.visibleDepositParts(-1));
        assertEquals(8, SilverVatBlockEntity.visibleDepositParts(128));
    }
}
