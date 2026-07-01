package com.emoniph.witchery.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsanityMobEffectTest {
    @Test
    void hallucinationChanceMirrorsSourceLevelThresholds() {
        assertEquals(35, InsanityMobEffect.hallucinationChance(1));
        assertEquals(30, InsanityMobEffect.hallucinationChance(2));
        assertEquals(25, InsanityMobEffect.hallucinationChance(3));
        assertEquals(25, InsanityMobEffect.hallucinationChance(4));
    }
}
