package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class DarkMarkRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceModelLocalScaleTranslationAndShadowArePreserved() {
        PoseStack actual = new PoseStack();
        DarkMarkRenderer.applySourceModelPose(actual);
        PoseStack expected = new PoseStack();
        expected.scale(2.0F, 2.0F, 2.0F);
        expected.translate(0.0F, -1.0F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
        assertEquals(0.5F, DarkMarkRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
