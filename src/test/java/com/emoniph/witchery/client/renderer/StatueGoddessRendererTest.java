package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class StatueGoddessRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyWorldFacingUsesTheExactSourceMatrix() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack actual = new PoseStack();
            StatueGoddessRenderer.applySourceWorldPose(actual, facing);
            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.translate(0.0F, -1.0F, 0.0F);
            expected.mulPose(Axis.YP.rotationDegrees(StatueGoddessRenderer.sourceRotation(facing)));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void sourceInventoryTesrPoseIsPreserved() {
        PoseStack actual = new PoseStack();
        StatueGoddessRenderer.applySourceItemPose(actual);
        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(0.0F, -1.0F, 0.0F);
        expected.scale(0.6F, 0.6F, 0.6F);
        expected.mulPose(Axis.YP.rotationDegrees(180.0F));
        expected.translate(0.0F, 1.5F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
