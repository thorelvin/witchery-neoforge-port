package com.emoniph.witchery.brewing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrewSplashMathTest {

    @Test
    void sourceInstantSplashUsesHalfDurationOnDirectEntityHits() {
        assertEquals(1.0, Brews.splashPowerFactor(0.0, true), 0.00001);
        assertEquals(1800, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(0.0, true)));
    }

    @Test
    void sourceInstantSplashFallsOffAcrossThreeBlockRadius() {
        assertEquals(0.5, Brews.splashPowerFactor(1.5, false), 0.00001);
        assertEquals(900, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(1.5, false)));
        assertEquals(0.0, Brews.splashPowerFactor(3.0, false), 0.00001);
        assertEquals(0, Brews.scaledSplashDuration(3600, Brews.splashPowerFactor(3.0, false)));
    }
}
