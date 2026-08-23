package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

class CoffinBlockTest {
    @Test
    void southFacingOpenCollisionMatchesSourceWallsAndHinge() {
        VoxelShape foot = CoffinBlock.sourceOpenShape(Direction.SOUTH, BedPart.FOOT);
        assertTrue(contains(foot, 0.5, 0.2, 0.5));
        assertTrue(contains(foot, 0.5, 0.6, 0.025));
        assertFalse(contains(foot, 0.5, 0.6, 0.975));
        assertTrue(contains(foot, 0.975, 1.5, 0.5));
        assertFalse(contains(foot, 0.025, 1.5, 0.5));

        VoxelShape head = CoffinBlock.sourceOpenShape(Direction.SOUTH, BedPart.HEAD);
        assertFalse(contains(head, 0.5, 0.6, 0.025));
        assertTrue(contains(head, 0.5, 0.6, 0.975));
    }

    private static boolean contains(VoxelShape shape, double x, double y, double z) {
        Vec3 point = new Vec3(x, y, z);
        return shape.toAabbs().stream().anyMatch(box -> box.contains(point));
    }
}
