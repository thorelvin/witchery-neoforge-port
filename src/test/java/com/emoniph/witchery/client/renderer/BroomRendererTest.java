package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class BroomRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sourceYawAndHitWobbleUseTheOriginalOrder() {
        PoseStack actual = new PoseStack();
        BroomRenderer.applySourceRotations(actual, 37.0F, -12.5F);
        PoseStack expected = new PoseStack();
        expected.mulPose(Axis.YP.rotationDegrees(53.0F));
        expected.mulPose(Axis.XP.rotationDegrees(-12.5F));
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void hitWobbleAndLivingRendererVerticalCompensationMatchTheSource() {
        float expectedWobble = Mth.sin(6.75F) * 6.75F * 13.75F / 10.0F * -1.0F;
        assertEquals(expectedWobble, BroomRenderer.sourceHitWobble(7, 14.0F, -1, 0.25F), EPSILON);
        assertEquals(0.0F, BroomRenderer.sourceHitWobble(0, 14.0F, -1, 0.25F), EPSILON);

        PoseStack pose = new PoseStack();
        BroomRenderer.applyLivingRendererCompensation(pose);
        assertEquals(BroomRenderer.LIVING_RENDERER_VERTICAL_COMPENSATION,
                pose.last().pose().get(3, 1), EPSILON);
        assertEquals(0.5F, BroomRenderer.SOURCE_SHADOW_RADIUS, EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
