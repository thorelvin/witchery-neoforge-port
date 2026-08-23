package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class GarlicGarlandBlockTest {
    private static final double EPSILON = 0.00001;

    @Test
    void selectionShapesPreserveTheSourceTopWallSlabs() {
        assertBounds(Direction.NORTH, 0.1, 0.8, 0.85, 0.9, 1.0, 1.0);
        assertBounds(Direction.SOUTH, 0.1, 0.8, 0.0, 0.9, 1.0, 0.15);
        assertBounds(Direction.WEST, 0.85, 0.8, 0.1, 1.0, 1.0, 0.9);
        assertBounds(Direction.EAST, 0.0, 0.8, 0.1, 0.15, 1.0, 0.9);
    }

    private static void assertBounds(Direction facing,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        AABB bounds = GarlicGarlandBlock.sourceShape(facing).bounds();
        assertEquals(minX, bounds.minX, EPSILON, facing + " minX");
        assertEquals(minY, bounds.minY, EPSILON, facing + " minY");
        assertEquals(minZ, bounds.minZ, EPSILON, facing + " minZ");
        assertEquals(maxX, bounds.maxX, EPSILON, facing + " maxX");
        assertEquals(maxY, bounds.maxY, EPSILON, facing + " maxY");
        assertEquals(maxZ, bounds.maxZ, EPSILON, facing + " maxZ");
    }
}
