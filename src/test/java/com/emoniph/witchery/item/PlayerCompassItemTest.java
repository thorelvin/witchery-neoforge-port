package com.emoniph.witchery.item;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerCompassItemTest {
    private static final Vec3 ORIGIN = Vec3.ZERO;

    @Test
    void compassFrameMatchesOriginalWitcheryCardinals() {
        assertEquals(0, PlayerCompassItem.compassFrame(90.0F, ORIGIN, new Vec3(1.0, 0.0, 0.0)));
        assertEquals(8, PlayerCompassItem.compassFrame(90.0F, ORIGIN, new Vec3(0.0, 0.0, 1.0)));
        assertEquals(16, PlayerCompassItem.compassFrame(90.0F, ORIGIN, new Vec3(-1.0, 0.0, 0.0)));
        assertEquals(24, PlayerCompassItem.compassFrame(90.0F, ORIGIN, new Vec3(0.0, 0.0, -1.0)));
    }

    @Test
    void compassAngleIsNormalizedToRangeDispatchThresholds() {
        assertEquals(0.25F, PlayerCompassItem.compassAngle(90.0F, ORIGIN, new Vec3(0.0, 0.0, 1.0)));
        assertEquals(0, PlayerCompassItem.compassFrame(450.0F, ORIGIN, new Vec3(1.0, 0.0, 0.0)));
    }
}
