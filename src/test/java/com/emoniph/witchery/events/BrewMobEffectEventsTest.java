package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrewMobEffectEventsTest {

    @Test
    void customDamageEffectMathMatchesSource() {
        assertEquals(1.0F, BrewMobEffectEvents.sourceWrappedMagicMultiplier(0));
        assertEquals(4.0F, BrewMobEffectEvents.sourceWrappedMagicMultiplier(8));
        assertEquals(0.2F, BrewMobEffectEvents.sourceAbsorbMagicFraction(0));
        assertEquals(0.8F, BrewMobEffectEvents.sourceAbsorbMagicFraction(3));
        assertEquals(2.0F, BrewMobEffectEvents.sourceReflectedDamage(12.0F, 0, false));
        assertEquals(0.0F, BrewMobEffectEvents.sourceReflectedDamage(12.0F, 1, true));
        assertEquals(4.0F, BrewMobEffectEvents.sourceReflectedDamage(12.0F, 3, true));
        assertEquals(2.0F, BrewMobEffectEvents.sourceVolatilityRadius(0));
        assertEquals(3.0F, BrewMobEffectEvents.sourceVolatilityRadius(8));
    }
}
