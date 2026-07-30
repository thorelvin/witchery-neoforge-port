package com.emoniph.witchery.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MirrorFaceEntityTest {
    @Test
    void sourceLifetimeUsesStrictGreaterThanTenSecondsGate() {
        assertEquals(200, MirrorFaceEntity.SOURCE_LIFETIME_TICKS);
        assertFalse(MirrorFaceEntity.sourceShouldDiscard(200));
        assertTrue(MirrorFaceEntity.sourceShouldDiscard(201));
    }

    @Test
    void sourcePositionFloatsJustOutsideTheReflectiveFace() {
        BlockPos mirror = new BlockPos(10, 64, -3);

        assertEquals(new Vec3(10.5D, 64.1D, -2.1D),
                MirrorFaceEntity.sourcePosition(mirror, Direction.SOUTH));
        assertEquals(new Vec3(10.1D, 64.1D, -2.5D),
                MirrorFaceEntity.sourcePosition(mirror, Direction.WEST));
    }
}
