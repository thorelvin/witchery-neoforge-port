package com.emoniph.witchery.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MirrorWorldLayoutTest {
    @Test
    void roomIndexesFollowSourceVerticalThenSpiralOrder() {
        assertEquals(new MirrorWorldLayout.RoomCoordinates(0, 0, 0),
                MirrorWorldLayout.roomForIndex(0));
        assertEquals(new MirrorWorldLayout.RoomCoordinates(0, 14, 0),
                MirrorWorldLayout.roomForIndex(14));
        assertEquals(new MirrorWorldLayout.RoomCoordinates(0, 0, -1),
                MirrorWorldLayout.roomForIndex(15));
        assertEquals(new MirrorWorldLayout.RoomCoordinates(-1, 0, -1),
                MirrorWorldLayout.roomForIndex(30));
        assertEquals(32, MirrorWorldLayout.indexForRoom(-1, 2, -1));
        assertEquals(-1, MirrorWorldLayout.indexForRoom(0, 15, 0));
        assertThrows(IllegalArgumentException.class, () -> MirrorWorldLayout.roomForIndex(-1));
    }

    @Test
    void generatedRoomHasExactNineCubedInterior() {
        int interior = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < MirrorWorldLayout.WORLD_HEIGHT; y++) {
                    if (MirrorWorldLayout.isInterior(x, y, z)) {
                        interior++;
                    }
                }
            }
        }
        assertEquals(16 * 9 * 9 * 9, interior);
        assertTrue(MirrorWorldLayout.isInterior(4, 6, 4));
        assertTrue(MirrorWorldLayout.isInterior(12, 14, 12));
        assertFalse(MirrorWorldLayout.isInterior(3, 6, 4));
        assertFalse(MirrorWorldLayout.isInterior(4, 15, 4));
        assertTrue(MirrorWorldLayout.isInterior(4, 22, 4));
        assertTrue(MirrorWorldLayout.isGeneratedAir(0, 255, 0));
        assertFalse(MirrorWorldLayout.isInterior(0, 255, 0));
    }
}
