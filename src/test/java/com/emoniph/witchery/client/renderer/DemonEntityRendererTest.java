package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.DemonEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class DemonEntityRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceWalkingRollIsAppliedAtTheRendererLayerOnlyWhileMoving() {
        PoseStack actual = new PoseStack();
        DemonEntityRenderer.applySourceWalkingRoll(actual, 4.0F, 0.7F);
        PoseStack expected = new PoseStack();
        float roll = 6.5F * DemonEntityModel.triangleWave(10.0F, 13.0F);
        expected.mulPose(Axis.ZP.rotationDegrees(roll));
        assertMatrixEquals(expected.last().pose(), actual.last().pose());

        PoseStack idle = new PoseStack();
        DemonEntityRenderer.applySourceWalkingRoll(idle, 4.0F, 0.009F);
        assertMatrixEquals(new Matrix4f(), idle.last().pose());
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
