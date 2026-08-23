package com.emoniph.witchery.blocks.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class MirrorBlockEntityTest {
    private static final double EPSILON = 0.00001D;
    private static final BlockPos ORIGIN = new BlockPos(10, 20, 30);

    @Test
    void livingEntityDetectionPrismsMatchTheSourceMetadataDirections() {
        assertBounds(Direction.NORTH, 9, 20, 26, 12, 21, 31);
        assertBounds(Direction.SOUTH, 9, 20, 30, 12, 21, 35);
        assertBounds(Direction.WEST, 6, 20, 29, 11, 21, 32);
        assertBounds(Direction.EAST, 10, 20, 29, 15, 21, 32);
    }

    private static void assertBounds(Direction facing,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        AABB bounds = MirrorBlockEntity.sourceDetectionBounds(ORIGIN, facing);
        assertEquals(minX, bounds.minX, EPSILON);
        assertEquals(minY, bounds.minY, EPSILON);
        assertEquals(minZ, bounds.minZ, EPSILON);
        assertEquals(maxX, bounds.maxX, EPSILON);
        assertEquals(maxY, bounds.maxY, EPSILON);
        assertEquals(maxZ, bounds.maxZ, EPSILON);
    }
}
