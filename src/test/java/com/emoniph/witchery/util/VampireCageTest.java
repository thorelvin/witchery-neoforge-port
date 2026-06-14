package com.emoniph.witchery.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the source {@code GenericEvents.isCaged} geometry (the level-8 vampire's iron-bar holding
 * cell): at least 15 of the 16 ring positions across two body layers must be iron bars, and the full
 * 3×3 roof layer must be solid. Pure geometry — no world needed, via a fake {@link VampireCage.BlockQuery}.
 */
class VampireCageTest {

    /** A fake world: a set of "iron bars" coordinates and a set of "solid roof" coordinates. */
    private record FakeWorld(Set<Long> bars, Set<Long> solid) implements VampireCage.BlockQuery {
        static long key(int x, int y, int z) {
            return (((long) x) & 0x1FFFFF) | ((((long) y) & 0x1FFFFF) << 21) | ((((long) z) & 0x1FFFFF) << 42);
        }
        @Override public boolean isIronBars(int x, int y, int z) { return bars.contains(key(x, y, z)); }
        @Override public boolean isSolidRoof(int x, int y, int z) { return solid.contains(key(x, y, z)); }
    }

    /** Build a fully sealed cage centred on the column at (cx,cy,cz). */
    private static FakeWorld sealedCage(int cx, int cy, int cz) {
        Set<Long> bars = new HashSet<>();
        Set<Long> solid = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;            // ring excludes the centre
                bars.add(FakeWorld.key(cx + dx, cy, cz + dz));      // body layer y
                bars.add(FakeWorld.key(cx + dx, cy + 1, cz + dz));  // body layer y+1
            }
            // roof layer y+2 covers the full 3×3 incl. centre
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                solid.add(FakeWorld.key(cx + dx, cy + 2, cz + dz));
            }
        }
        return new FakeWorld(bars, solid);
    }

    @Test
    void aFullySealedCageIsDetected() {
        FakeWorld w = sealedCage(0, 64, 0);
        assertTrue(VampireCage.isCaged(0, 64, 0, w));
        assertTrue(VampireCage.isCagedAround(0, 64, 0, w));
    }

    @Test
    void missingOneBarStillCounts_butMissingTwoFails() {
        FakeWorld full = sealedCage(0, 64, 0);
        // 16 ring bars; remove one → 15 → still a cage.
        full.bars().remove(FakeWorld.key(1, 64, 0));
        assertTrue(VampireCage.isCaged(0, 64, 0, full));
        // remove a second → 14 → no longer a cage.
        full.bars().remove(FakeWorld.key(-1, 64, 0));
        assertFalse(VampireCage.isCaged(0, 64, 0, full));
    }

    @Test
    void anOpenRoofIsNotACage() {
        FakeWorld w = sealedCage(0, 64, 0);
        w.solid().remove(FakeWorld.key(0, 66, 0)); // one hole in the lid
        assertFalse(VampireCage.isCaged(0, 64, 0, w));
    }

    @Test
    void emptySpaceIsNotACage() {
        FakeWorld empty = new FakeWorld(new HashSet<>(), new HashSet<>());
        assertFalse(VampireCage.isCagedAround(0, 64, 0, empty));
    }

    @Test
    void cageDetectedWhenVillagerStandsInANeighbourColumn() {
        // Cage built around (1,64,0); the villager column reported as (0,64,0) is one of the 8 neighbours.
        FakeWorld w = sealedCage(1, 64, 0);
        assertFalse(VampireCage.isCaged(0, 64, 0, w));
        assertTrue(VampireCage.isCagedAround(0, 64, 0, w));
    }
}
