package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class BrazierRendererTest {
    @Test
    void worldRendererUsesExactSourceTesrMatrix() {
        PoseStack actual = new PoseStack();
        BrazierRenderer.applySourcePose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(0.0F, -1.0F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), 0.00001F,
                        "matrix[" + column + "][" + row + "]");
            }
        }
    }
}
