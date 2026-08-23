package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class PoppetShelfRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void shelfRendererUsesTheExactMetadataIndependentSourceMatrix() {
        PoseStack actual = new PoseStack();
        PoppetShelfRenderer.applySourceShelfPose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(-0.5F, 0.0F, -0.5F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void modernSlotPositionsAreAlgebraicallyIdenticalToTheScaledSourceGrid() {
        for (int slot = 0; slot < 9; slot++) {
            float sourceXShift = (slot % 3 + 1) * 0.5F;
            float sourceZShift = (slot / 3 + 1) * 0.5F;

            PoseStack actual = new PoseStack();
            PoppetShelfRenderer.applyStoredPoppetPose(actual, slot, 42.0F);

            PoseStack expected = new PoseStack();
            expected.translate(0.0F, 0.6F, 0.0F);
            expected.scale(0.5F, 0.5F, 0.5F);
            expected.translate(sourceXShift, 0.0F, sourceZShift);
            expected.mulPose(Axis.YP.rotationDegrees(42.0F));
            expected.mulPose(Axis.XP.rotationDegrees(-90.0F));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void sourceWallClockSpinRateIsPreserved() {
        assertEquals(0.0F, PoppetShelfRenderer.sourceSpinDegrees(0L), EPSILON);
        assertEquals(50.0F, PoppetShelfRenderer.sourceSpinDegrees(1_000L), EPSILON);
        assertEquals(0.0F, PoppetShelfRenderer.sourceSpinDegrees(7_200L), EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
