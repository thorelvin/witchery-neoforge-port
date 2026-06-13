package com.emoniph.witchery.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.Identifier;
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

    @Test
    void indexLineHitTestingMatchesPlacedRendererSpacing() {
        PlacedModonomiconBookResolver.PlacedIndexPage page = new PlacedModonomiconBookResolver.PlacedIndexPage(
                "Book",
                List.of(
                        PlacedModonomiconBookResolver.IndexLine.category("Getting Started"),
                        PlacedModonomiconBookResolver.IndexLine.entry("First Steps", Identifier.parse("witchery:first_steps")),
                        PlacedModonomiconBookResolver.IndexLine.category("Herbology"),
                        PlacedModonomiconBookResolver.IndexLine.entry("Rowan", Identifier.parse("witchery:rowan"))));

        assertEquals(0, PlacedModonomiconBookResolver.indexLineAtY(page, 19));
        assertEquals(1, PlacedModonomiconBookResolver.indexLineAtY(page, 26));
        assertEquals(2, PlacedModonomiconBookResolver.indexLineAtY(page, 35));
        assertEquals(3, PlacedModonomiconBookResolver.indexLineAtY(page, 42));
        assertEquals(-1, PlacedModonomiconBookResolver.indexLineAtY(page, 120));
    }
}
