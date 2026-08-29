package com.emoniph.witchery.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlyingMonkeyPreparationTest {

    @Test
    void capacityAndCooldownBoundNewPreparationJobs() {
        assertTrue(FlyingMonkeyEntity.canStartPreparation(0, 100L, 100L));
        assertFalse(FlyingMonkeyEntity.canStartPreparation(8, 100L, 100L));
        assertFalse(FlyingMonkeyEntity.canStartPreparation(0, 101L, 100L));
    }

    @Test
    void preparationHasDeterministicTimeout() {
        assertFalse(FlyingMonkeyEntity.preparationTimedOut(100L, 400L));
        assertTrue(FlyingMonkeyEntity.preparationTimedOut(100L, 401L));
        assertTrue(FlyingMonkeyEntity.preparationTimedOut(Long.MIN_VALUE, 100L));
    }
}
