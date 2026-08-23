package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class WolfAltarBlockTest {
    private static final double EPSILON = 0.00001;

    @Test
    void sourceSelectionAndCollisionBoundsRemainTwoBlocksTall() {
        AABB bounds = WolfAltarBlock.sourceShape().bounds();
        assertEquals(0.0, bounds.minX, EPSILON);
        assertEquals(0.0, bounds.minY, EPSILON);
        assertEquals(0.0, bounds.minZ, EPSILON);
        assertEquals(1.0, bounds.maxX, EPSILON);
        assertEquals(2.0, bounds.maxY, EPSILON);
        assertEquals(1.0, bounds.maxZ, EPSILON);
    }
}
