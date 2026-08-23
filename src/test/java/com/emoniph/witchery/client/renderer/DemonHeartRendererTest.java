package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class DemonHeartRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyFacingUsesTheExactSourceMetadataRotationAndMatrixOrder() {
        assertEquals(0.0F, DemonHeartRenderer.sourceRotation(Direction.NORTH));
        assertEquals(90.0F, DemonHeartRenderer.sourceRotation(Direction.EAST));
        assertEquals(180.0F, DemonHeartRenderer.sourceRotation(Direction.SOUTH));
        assertEquals(270.0F, DemonHeartRenderer.sourceRotation(Direction.WEST));

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            float yaw = DemonHeartRenderer.sourceRotation(facing);
            PoseStack actual = new PoseStack();
            DemonHeartRenderer.applySourcePose(actual, yaw);

            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.scale(0.7F, 0.7F, 0.7F);
            expected.mulPose(Axis.YP.rotationDegrees(yaw));
            expected.translate(0.1F, -0.9F, -0.15F);
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void sourceTwentyFiveTickPulseIsPreserved() {
        assertEquals(1.155F, DemonHeartRenderer.sourcePulse(0), EPSILON);
        assertEquals(1.19625F, DemonHeartRenderer.sourcePulse(6), 0.0001F);
        assertEquals(1.155F, DemonHeartRenderer.sourcePulse(25), EPSILON);
        assertEquals(DemonHeartRenderer.sourcePulse(3), DemonHeartRenderer.sourcePulse(28), EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
