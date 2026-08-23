package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class GrassperRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyFacingUsesTheExactSourceMetadataRotationAndMatrix() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            float rotation = GrassperRenderer.sourceRotation(facing);
            PoseStack actual = new PoseStack();
            GrassperRenderer.applySourcePose(actual, rotation);
            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.translate(0.0F, -1.0F, 0.0F);
            expected.mulPose(Axis.YP.rotationDegrees(rotation));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
        assertEquals(0.0F, GrassperRenderer.sourceRotation(Direction.NORTH));
        assertEquals(90.0F, GrassperRenderer.sourceRotation(Direction.EAST));
        assertEquals(180.0F, GrassperRenderer.sourceRotation(Direction.SOUTH));
        assertEquals(270.0F, GrassperRenderer.sourceRotation(Direction.WEST));
    }

    @Test
    void heldItemSpinUsesTheSourceWallClockRate() {
        assertEquals(0.0F, GrassperRenderer.sourceHeldItemRotation(0), EPSILON);
        assertEquals(150.0F, GrassperRenderer.sourceHeldItemRotation(3000), EPSILON);
        assertEquals(300.0F, GrassperRenderer.sourceHeldItemRotation(6000), EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
