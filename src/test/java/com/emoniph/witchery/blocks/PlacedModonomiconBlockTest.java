package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PlacedModonomiconBlockTest {
    private static final Vec3 PAGE_CENTER = new Vec3(0.0D, 0.55D, 0.0D);
    private static final Vec3 FORWARD = new Vec3(0.0D, 0.0D, 1.0D);
    private static final Vec3 EYE = new Vec3(0.0D, 1.6D, 3.0D);

    @Test
    void pageControlHitsBottomRightNextZone() {
        assertEquals(1, directionToward(0.3D, 0.35D, 0.0D));
    }

    @Test
    void pageControlHitsBottomLeftBackZone() {
        assertEquals(-1, directionToward(-0.3D, 0.35D, 0.0D));
    }

    @Test
    void pageControlIgnoresPageBodyAndOuterMisses() {
        assertEquals(0, directionToward(0.0D, 0.35D, 0.0D), "center fold is not a page control");
        assertEquals(0, directionToward(0.3D, 0.55D, 0.0D), "upper page content is not a page control");
        assertEquals(0, directionToward(0.6D, 0.35D, 0.0D), "outside the page is not a page control");
    }

    @Test
    void pageControlIgnoresParallelLookRay() {
        assertEquals(0, PlacedModonomiconBlock.pageControlDirection(FORWARD, PAGE_CENTER, EYE, new Vec3(1.0D, 0.0D, 0.0D)));
    }

    @Test
    void pageControlsAreRelativeToReaderDirection() {
        Vec3 forwardEast = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 eyeEast = new Vec3(3.0D, 1.6D, 0.0D);

        assertEquals(1, PlacedModonomiconBlock.pageControlDirection(
                forwardEast, PAGE_CENTER, eyeEast, new Vec3(0.0D, 0.35D, -0.3D).subtract(eyeEast)));
        assertEquals(-1, PlacedModonomiconBlock.pageControlDirection(
                forwardEast, PAGE_CENTER, eyeEast, new Vec3(0.0D, 0.35D, 0.3D).subtract(eyeEast)));
    }

    @Test
    void pageControlIgnoresLookRaysBehindThePlayer() {
        assertEquals(0, PlacedModonomiconBlock.pageControlDirection(
                FORWARD, PAGE_CENTER, EYE, new Vec3(0.0D, 0.0D, 1.0D)));
    }

    private static int directionToward(double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        return PlacedModonomiconBlock.pageControlDirection(FORWARD, PAGE_CENTER, EYE, target.subtract(EYE));
    }
}
