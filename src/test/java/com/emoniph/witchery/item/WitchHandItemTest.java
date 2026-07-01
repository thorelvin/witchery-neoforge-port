package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitchHandItemTest {
    @Test
    void lightBendUsesSourceDurationAndAggroDropBounds() {
        AABB area = WitchHandItem.lightAggroDropArea(10.0D, 64.0D, -3.0D);

        assertEquals(30, WitchHandItem.LIGHT_BEND_INVISIBILITY_TICKS);
        assertEquals(-10.0D, area.minX);
        assertEquals(64.0D, area.minY);
        assertEquals(-23.0D, area.minZ);
        assertEquals(30.0D, area.maxX);
        assertEquals(66.0D, area.maxY);
        assertEquals(17.0D, area.maxZ);
    }

    @Test
    void lightHoistCageUsesSourceWideCircleShape() {
        BlockPos center = new BlockPos(10, 68, -3);
        Set<BlockPos> positions = WitchHandItem.lightHoistCagePositions(center);

        assertEquals(78, positions.size());
        assertTrue(positions.contains(center.below().north(2)));
        assertTrue(positions.contains(center.below().north().east()));
        assertTrue(positions.contains(center.north(2)));
        assertTrue(positions.contains(center.above().east(2).south()));
        assertTrue(positions.contains(center.above(3).south(2)));
        assertTrue(positions.contains(center.above(3).north().west()));
        assertFalse(positions.contains(center));
        assertFalse(positions.contains(center.above().north().east()));
    }

    @Test
    void lightHoistClearanceChecksSourceCardinalBodySpace() {
        BlockPos center = new BlockPos(10, 68, -3);
        Set<BlockPos> positions = WitchHandItem.lightHoistClearancePositions(center);

        assertEquals(15, positions.size());
        assertTrue(positions.contains(center));
        assertTrue(positions.contains(center.above(2).north()));
        assertTrue(positions.contains(center.above().east()));
        assertFalse(positions.contains(center.north().east()));
        assertFalse(positions.contains(center.above(3)));
    }

    @Test
    void lightQuickReleaseEntityCageUsesSourceCircleShape() {
        BlockPos center = new BlockPos(10, 64, -3);
        Set<BlockPos> positions = WitchHandItem.lightReleaseEntityCagePositions(center);

        assertEquals(62, positions.size());
        assertTrue(positions.contains(center.below()));
        assertTrue(positions.contains(center.below().north()));
        assertTrue(positions.contains(center.below().south()));
        assertTrue(positions.contains(center.below().east()));
        assertTrue(positions.contains(center.below().west()));
        assertFalse(positions.contains(center.below().north().east()));

        assertTrue(positions.contains(center.north(2)));
        assertTrue(positions.contains(center.above().east(2).south()));
        assertTrue(positions.contains(center.above(2).west().south(2)));
        assertFalse(positions.contains(center.above()));
        assertFalse(positions.contains(center.above().north().east()));

        assertTrue(positions.contains(center.above(3).north(2)));
        assertTrue(positions.contains(center.above(3).north().east()));
    }

    @Test
    void lightQuickReleaseSideFaceSproutsFifteenBlocksOutward() {
        BlockPos hit = new BlockPos(10, 64, -3);
        Set<BlockPos> positions = WitchHandItem.lightBarrierLinePositions(hit, Direction.NORTH);

        assertEquals(15, positions.size());
        assertFalse(positions.contains(hit));
        assertTrue(positions.contains(hit.north()));
        assertTrue(positions.contains(hit.north(15)));
        assertFalse(positions.contains(hit.north(16)));
    }

    @Test
    void lightBarrierLineDoesNotReplaceTopFaceShieldGeometry() {
        assertTrue(WitchHandItem.lightBarrierLinePositions(new BlockPos(0, 64, 0), Direction.UP).isEmpty());
    }

    @Test
    void infernalSneakReleaseOnlyCommandsTopFaceDestinations() {
        BlockPos hit = new BlockPos(4, 70, -9);
        BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(hit), Direction.UP, hit, false);
        BlockHitResult side = new BlockHitResult(Vec3.atCenterOf(hit), Direction.NORTH, hit, false);

        assertEquals(hit.above(), WitchHandItem.infernalCommandDestination(top));
        assertNull(WitchHandItem.infernalCommandDestination(side));
    }

    @Test
    void otherwhereNormalReleaseAcceptsEntityHitLocations() {
        Vec3 location = new Vec3(2.25D, 65.0D, -4.75D);
        HitResult hit = new HitResult(location) {
            @Override
            public Type getType() {
                return Type.ENTITY;
            }
        };
        HitResult miss = BlockHitResult.miss(location, Direction.UP, new BlockPos(2, 65, -5));

        assertEquals(location, WitchHandItem.otherwhereEntityTeleportDestination(hit));
        assertNull(WitchHandItem.otherwhereEntityTeleportDestination(miss));
    }
}
