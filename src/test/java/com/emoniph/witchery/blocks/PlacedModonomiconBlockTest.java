package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlacedModonomiconBlockTest {
    @Test
    void pageControlHitsBottomRightNextZone() {
        assertEquals(1, PlacedModonomiconBlock.pageControlDirection(
                new PlacedModonomiconBlock.PageHit(false, 101.0D, 141.0D, 1.0D)));
    }

    @Test
    void pageControlHitsBottomLeftBackZone() {
        assertEquals(-1, PlacedModonomiconBlock.pageControlDirection(
                new PlacedModonomiconBlock.PageHit(true, 16.0D, 141.0D, 1.0D)));
    }

    @Test
    void pageControlIgnoresPageBodyAndOuterMisses() {
        assertEquals(0, PlacedModonomiconBlock.pageControlDirection(null), "misses are not page controls");
        assertEquals(0, PlacedModonomiconBlock.pageControlDirection(
                new PlacedModonomiconBlock.PageHit(false, 60.0D, 141.0D, 1.0D)), "bottom page body is not a page control");
        assertEquals(0, PlacedModonomiconBlock.pageControlDirection(
                new PlacedModonomiconBlock.PageHit(false, 101.0D, 20.0D, 1.0D)), "upper page content is not a page control");
        assertEquals(0, PlacedModonomiconBlock.pageControlDirection(
                new PlacedModonomiconBlock.PageHit(true, 101.0D, 141.0D, 1.0D)), "next control only lives on the right page");
    }

    @Test
    void centerBottomHitsEntriesButtonZone() {
        assertTrue(PlacedModonomiconBlock.hitsIndexButton(new PlacedModonomiconBlock.PageHit(true, 114.0D, 142.0D, 1.0D)));
        assertTrue(PlacedModonomiconBlock.hitsIndexButton(new PlacedModonomiconBlock.PageHit(false, 10.0D, 142.0D, 1.0D)));
        assertFalse(PlacedModonomiconBlock.hitsIndexButton(new PlacedModonomiconBlock.PageHit(false, 60.0D, 142.0D, 1.0D)));
        assertFalse(PlacedModonomiconBlock.hitsIndexButton(new PlacedModonomiconBlock.PageHit(false, 10.0D, 80.0D, 1.0D)));
    }

    @Test
    void pageVerticalHitConvertsToBookPixels() {
        assertEquals(0, PlacedModonomiconBlock.pagePixelY(0.0D));
        assertEquals(78, PlacedModonomiconBlock.pagePixelY(78.0D));
        assertEquals(155, PlacedModonomiconBlock.pagePixelY(155.0D));
    }

    @Test
    void pageHorizontalHitConvertsToBookPixels() {
        assertEquals(0, PlacedModonomiconBlock.pagePixelX(
                new PlacedModonomiconBlock.PageHit(true, 0.0D, 0.0D, 1.0D)));
        assertEquals(62, PlacedModonomiconBlock.pagePixelX(
                new PlacedModonomiconBlock.PageHit(false, 62.0D, 0.0D, 1.0D)));
        assertEquals(124, PlacedModonomiconBlock.pagePixelX(
                new PlacedModonomiconBlock.PageHit(false, 124.0D, 0.0D, 1.0D)));
    }
}
