package com.emoniph.witchery.ritual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentExpandingRiteParityTest {
    @Test
    void fertilityKeepsSourceExpansionGeometryAndCadence() {
        assertEquals(50, RitualExecutor.FERTILITY_BASE_RADIUS);
        assertEquals(15, RitualExecutor.FERTILITY_HEIGHT);
        assertEquals(5, RitualExecutor.FERTILITY_PULSE_INTERVAL);
        assertEquals(50, RitualExecutor.fertilityMaxRadius(0));
        assertEquals(62, RitualExecutor.fertilityMaxRadius(6));
    }

    @Test
    void forestationKeepsSourcePulseGeometryAndCovenScaling() {
        assertEquals(20, RitualExecutor.FORESTATION_BASE_RADIUS);
        assertEquals(8, RitualExecutor.FORESTATION_HEIGHT);
        assertEquals(60, RitualExecutor.FORESTATION_BASE_DURATION);
        assertEquals(20, RitualExecutor.FORESTATION_PULSE_INTERVAL);
        assertEquals(11, RitualExecutor.FORESTATION_BONEMEAL_ATTEMPTS);
        assertEquals(20, RitualExecutor.forestationRadius(0));
        assertEquals(32, RitualExecutor.forestationRadius(6));
        assertEquals(60, RitualExecutor.forestationDuration(0));
        assertEquals(90, RitualExecutor.forestationDuration(6));
    }
}
