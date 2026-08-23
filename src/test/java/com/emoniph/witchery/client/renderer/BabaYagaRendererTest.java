package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class BabaYagaRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceScaleShadowTextureAndExactLegacyVerticalAnchorArePreserved() {
        assertEquals(0.5F, BabaYagaRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
        assertEquals(0.9375F, BabaYagaRenderer.SOURCE_SCALE, EPSILON);
        assertEquals(-0.2068125F, BabaYagaRenderer.SOURCE_VERTICAL_COMPENSATION, EPSILON);
        assertEquals("witchery:textures/entity/babayaga.png", BabaYagaRenderer.TEXTURE.toString());

        PoseStack pose = new PoseStack();
        BabaYagaRenderer.applySourceScaleAndVerticalAnchor(pose);
        pose.translate(0.0F, BabaYagaRenderer.MODERN_LIVING_BASE_Y, 0.0F);
        assertEquals(BabaYagaRenderer.SOURCE_SCALE,
                pose.last().pose().get(0, 0), EPSILON);
        assertEquals(BabaYagaRenderer.SOURCE_SCALE
                        * (BabaYagaRenderer.LEGACY_LIVING_BASE_Y
                        + BabaYagaRenderer.SOURCE_MODEL_TRANSLATION_Y),
                pose.last().pose().get(3, 1), EPSILON);
    }

    @Test
    void flatHeldItemTransformUsesTheSourceOperationOrder() {
        PoseStack actual = new PoseStack();
        BabaYagaHeldItemLayer.applyCommonSourceTransform(actual);

        PoseStack expected = new PoseStack();
        expected.translate(-0.0625F, 0.53125F, 0.21875F);
        expected.translate(0.25F, 0.1875F, -0.1875F);
        expected.scale(0.375F, 0.375F, 0.375F);
        expected.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(60.0F));
        expected.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
        expected.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(20.0F));
        expected.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-15.0F));
        expected.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(40.0F));
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
