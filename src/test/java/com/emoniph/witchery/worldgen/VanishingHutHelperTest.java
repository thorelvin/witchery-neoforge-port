package com.emoniph.witchery.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishingHutHelperTest {

    @Test
    void relocatesOnlyWhenExitedUnwatchedOffCooldown() {
        long cd = VanishingHutHelper.DEFAULT_MOVE_COOLDOWN_TICKS;
        // Happy path: nobody inside, a primed player outside, unwatched, not held, cooldown elapsed.
        assertTrue(VanishingHutHelper.shouldRelocate(false, true, false, false, cd, cd));
    }

    @Test
    void doesNotRelocateWhileInsideWatchedHeldOrPrimedAbsentOrOnCooldown() {
        long cd = VanishingHutHelper.DEFAULT_MOVE_COOLDOWN_TICKS;
        assertFalse(VanishingHutHelper.shouldRelocate(true, true, false, false, cd, cd), "player inside blocks");
        assertFalse(VanishingHutHelper.shouldRelocate(false, false, false, false, cd, cd), "nobody primed blocks");
        assertFalse(VanishingHutHelper.shouldRelocate(false, true, true, false, cd, cd), "watched blocks");
        assertFalse(VanishingHutHelper.shouldRelocate(false, true, false, true, cd, cd), "held in place blocks");
        assertFalse(VanishingHutHelper.shouldRelocate(false, true, false, false, cd - 1, cd), "cooldown blocks");
    }

    @Test
    void lookConeAcceptsAheadAndRejectsBehindOrToTheSide() {
        Vec3 eye = new Vec3(0, 0, 0);
        Vec3 lookSouth = new Vec3(0, 0, 1); // looking +Z
        assertTrue(VanishingHutHelper.isLookingAt(eye, lookSouth, new Vec3(0, 0, 10)), "directly ahead");
        assertTrue(VanishingHutHelper.isLookingAt(eye, lookSouth, new Vec3(3, 0, 10)), "slightly off-axis, in cone");
        assertFalse(VanishingHutHelper.isLookingAt(eye, lookSouth, new Vec3(0, 0, -10)), "directly behind");
        assertFalse(VanishingHutHelper.isLookingAt(eye, lookSouth, new Vec3(10, 0, 0)), "90 degrees to the side");
    }

    @Test
    void crossDimensionRollHonoursBounds() {
        RandomSource rng = RandomSource.create(42L);
        assertFalse(VanishingHutHelper.pickCrossDimension(rng, 0.0D), "0 never crosses");
        assertTrue(VanishingHutHelper.pickCrossDimension(rng, 1.0D), "1 always crosses");
    }

    @Test
    void crossDimensionRollApproximatesChance() {
        RandomSource rng = RandomSource.create(123456L);
        int crossed = 0;
        int total = 100000;
        for (int i = 0; i < total; i++) {
            if (VanishingHutHelper.pickCrossDimension(rng, VanishingHutHelper.CROSS_DIMENSION_CHANCE)) {
                crossed++;
            }
        }
        double rate = (double) crossed / total;
        assertTrue(rate > 0.23D && rate < 0.27D, "expected ~25% cross-dim, got " + rate);
    }

    @Test
    void watchRangeSquaredMatchesRange() {
        assertEquals(VanishingHutHelper.WATCH_RANGE * VanishingHutHelper.WATCH_RANGE,
                VanishingHutHelper.WATCH_RANGE_SQ);
    }
}
