package com.emoniph.witchery.effect;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain-JVM regression lock for the Swim Boost (Cod) branch predicates + motion math, including the
 * non-player/paralysed branch now delivered server-side from {@link SwimmingMobEffect#applyEffectTick}.
 * Source: 1.7.10 {@code PotionSwimming.onLivingUpdate}.
 */
class SwimmingMobEffectTest {

    @Test
    void playerBoostsWhileSwimmingForwardUnlessParalysed() {
        assertTrue(SwimmingMobEffect.shouldBoostPlayer(true, true, false));
        assertFalse(SwimmingMobEffect.shouldBoostPlayer(true, true, true));   // paralysed excludes
        assertFalse(SwimmingMobEffect.shouldBoostPlayer(false, true, false)); // not in water
        assertFalse(SwimmingMobEffect.shouldBoostPlayer(true, false, false)); // not moving forward
    }

    @Test
    void nonPlayerBoostsEveryTenthTickOnlyWhenParalysedInWater() {
        assertTrue(SwimmingMobEffect.shouldBoostNonPlayer(3L, true, true));
        assertTrue(SwimmingMobEffect.shouldBoostNonPlayer(13L, true, true)); // 13 % 10 == 3
        assertFalse(SwimmingMobEffect.shouldBoostNonPlayer(4L, true, true));  // wrong phase
        assertFalse(SwimmingMobEffect.shouldBoostNonPlayer(3L, false, true)); // not in water
        assertFalse(SwimmingMobEffect.shouldBoostNonPlayer(3L, true, false)); // not paralysed
    }

    @Test
    void horizontalMultiplierScalesWithAmplifier() {
        assertEquals(1.15, SwimmingMobEffect.horizontalMultiplier(0), 1e-9);
        assertEquals(1.24, SwimmingMobEffect.horizontalMultiplier(3), 1e-9); // 1.15 + 0.03*3
    }

    @Test
    void boostHorizontalScalesXZAndPreservesY() {
        Vec3 boosted = SwimmingMobEffect.boostHorizontal(new Vec3(2.0, 5.0, -4.0), 0);
        assertEquals(2.3, boosted.x, 1e-9);   // 2.0 * 1.15
        assertEquals(5.0, boosted.y, 1e-9);   // vertical untouched
        assertEquals(-4.6, boosted.z, 1e-9);  // -4.0 * 1.15
    }
}
