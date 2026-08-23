package com.emoniph.witchery.blocks.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoffinBlockEntityTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceLidAnimationMovesByOneTenthAndClamps() {
        assertEquals(0.1F, CoffinBlockEntity.sourceNextLidAngle(0.0F, true), EPSILON);
        assertEquals(1.0F, CoffinBlockEntity.sourceNextLidAngle(0.95F, true), EPSILON);
        assertEquals(0.4F, CoffinBlockEntity.sourceNextLidAngle(0.5F, false), EPSILON);
        assertEquals(0.0F, CoffinBlockEntity.sourceNextLidAngle(0.05F, false), EPSILON);
    }
}
