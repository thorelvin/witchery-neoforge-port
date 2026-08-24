package com.emoniph.witchery.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomBrewMobEffectMathTest {

    @Test
    void resizingUsesTheFourSourceScaleAndJumpTiers() {
        assertEquals(0.25F, ResizingMobEffect.sourceScaleFactor(0));
        assertEquals(0.4F, ResizingMobEffect.sourceScaleFactor(1));
        assertEquals(2.0F, ResizingMobEffect.sourceScaleFactor(2));
        assertEquals(3.0F, ResizingMobEffect.sourceScaleFactor(3));
        assertEquals(0.75D, ResizingMobEffect.sourceJumpMultiplier(0));
        assertEquals(0.75D, ResizingMobEffect.sourceJumpMultiplier(1));
        assertEquals(1.5D, ResizingMobEffect.sourceJumpMultiplier(2));
        assertEquals(2.0D, ResizingMobEffect.sourceJumpMultiplier(3));
        assertEquals(2.0F / 3.0F, ResizingMobEffect.sourceMeleeDamageMultiplierForTiers(2, 3));
        assertEquals(4.0F / 3.0F, ResizingMobEffect.sourceMeleeDamageMultiplierForTiers(4, 3));
        assertEquals(3.0F, ResizingMobEffect.sourceMeleeDamageMultiplierForTiers(5, 1));
    }

    @Test
    void colorfulUsesTheExactWitcheryDyePalette() {
        int[] expected = {
                1118481, 12464176, 5732898, 5057301,
                2247599, 8532146, 3968688, 11513789,
                7763574, 15574987, 8639516, 15197994,
                12179199, 14383829, 15113780, 16777215
        };
        for (int amplifier = 0; amplifier < expected.length; amplifier++) {
            assertEquals(expected[amplifier], ColorfulMobEffect.sourceTint(amplifier));
        }
        assertEquals(expected[15], ColorfulMobEffect.sourceTint(99));
    }
}
