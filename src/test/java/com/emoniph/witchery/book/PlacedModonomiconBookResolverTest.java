package com.emoniph.witchery.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlacedModonomiconBookResolverTest {
    @Test
    void clampOpenPagesIndexKeepsEvenSpreadsInRange() {
        assertEquals(0, PlacedModonomiconBookResolver.clampOpenPagesIndex(-2, 7));
        assertEquals(0, PlacedModonomiconBookResolver.clampOpenPagesIndex(1, 7));
        assertEquals(2, PlacedModonomiconBookResolver.clampOpenPagesIndex(2, 7));
        assertEquals(6, PlacedModonomiconBookResolver.clampOpenPagesIndex(999, 7));
        assertEquals(4, PlacedModonomiconBookResolver.clampOpenPagesIndex(999, 6));
    }

    @Test
    void clampOpenPagesIndexHandlesEmptyBooks() {
        assertEquals(0, PlacedModonomiconBookResolver.clampOpenPagesIndex(4, 0));
        assertEquals(0, PlacedModonomiconBookResolver.clampOpenPagesIndex(4, -1));
    }

    @Test
    void oddEntriesGetSpacerOnlyWhenAnotherEntryFollows() {
        assertTrue(PlacedModonomiconBookResolver.shouldAddEntrySpacer(5, true));
        assertFalse(PlacedModonomiconBookResolver.shouldAddEntrySpacer(4, true));
        assertFalse(PlacedModonomiconBookResolver.shouldAddEntrySpacer(5, false));
    }
}
