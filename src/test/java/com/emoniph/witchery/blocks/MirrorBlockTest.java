package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

class MirrorBlockTest {
    private static final double EPSILON = 0.00001D;

    @Test
    void collisionShapesPreserveTheSourceWallSideAndDepth() {
        assertBounds(MirrorBlock.sourceCollisionShape(Direction.NORTH), 0, 0, 0.85, 1, 1, 1);
        assertBounds(MirrorBlock.sourceCollisionShape(Direction.SOUTH), 0, 0, 0, 1, 1, 0.15);
        assertBounds(MirrorBlock.sourceCollisionShape(Direction.WEST), 0.85, 0, 0, 1, 1, 1);
        assertBounds(MirrorBlock.sourceCollisionShape(Direction.EAST), 0, 0, 0, 0.15, 1, 1);
    }

    @Test
    void selectionShapesPreserveTheSourceWallSideAndDepth() {
        assertBounds(MirrorBlock.sourceSelectionShape(Direction.NORTH), 0, 0, 0.68, 1, 1, 1);
        assertBounds(MirrorBlock.sourceSelectionShape(Direction.SOUTH), 0, 0, 0, 1, 1, 0.32);
        assertBounds(MirrorBlock.sourceSelectionShape(Direction.WEST), 0.68, 0, 0, 1, 1, 1);
        assertBounds(MirrorBlock.sourceSelectionShape(Direction.EAST), 0, 0, 0, 0.32, 1, 1);
    }

    private static void assertBounds(VoxelShape shape,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        AABB bounds = shape.bounds();
        assertEquals(minX, bounds.minX, EPSILON);
        assertEquals(minY, bounds.minY, EPSILON);
        assertEquals(minZ, bounds.minZ, EPSILON);
        assertEquals(maxX, bounds.maxX, EPSILON);
        assertEquals(maxY, bounds.maxY, EPSILON);
        assertEquals(maxZ, bounds.maxZ, EPSILON);
    }
}
