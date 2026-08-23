package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VampireArmorChainLayerTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceChainPassUsesCumulativeSecondScaleAndArmPivotDelta() {
        assertEquals(1.06F, VampireArmorChainLayer.BODY_SCALE, EPSILON);
        assertEquals(1.1236F, VampireArmorChainLayer.ARM_SCALE, EPSILON);
        assertEquals(-0.05F, VampireArmorChainLayer.ARM_PIVOT_Y_DELTA, EPSILON);
    }
}
