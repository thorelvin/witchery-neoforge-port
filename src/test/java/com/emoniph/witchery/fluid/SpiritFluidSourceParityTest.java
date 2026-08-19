package com.emoniph.witchery.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpiritFluidSourceParityTest {
    @Test
    void sourceFluidPhysicsRemainDistinctFromWater() {
        assertEquals(500, SpiritFluid.SOURCE_DENSITY);
        assertEquals(2000, SpiritFluid.SOURCE_VISCOSITY);
        assertEquals(4, SpiritFluid.SOURCE_SLOPE_DISTANCE);
        assertEquals(2, SpiritFluid.SOURCE_LEVEL_DECREASE);
        assertEquals(10, SpiritFluid.SOURCE_TICK_RATE);
    }
}
