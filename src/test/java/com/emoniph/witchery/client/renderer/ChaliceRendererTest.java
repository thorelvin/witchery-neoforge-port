package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.ChaliceModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ChaliceRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void worldRendererUsesExactSourceTesrMatrix() {
        PoseStack actual = new PoseStack();
        ChaliceRenderer.applySourcePose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(0.0F, -1.0F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void baseAndLiquidOccupySourceWorldSpaceHeights() {
        ModelPart root = ChaliceModel.createBodyLayer().bakeRoot();
        ChaliceModel model = new ChaliceModel(root);

        PoseStack basePose = new PoseStack();
        ChaliceRenderer.applySourcePose(basePose);
        model.chalice().translateAndRotate(basePose);
        Vector3f baseBottom = basePose.last().pose().transformPosition(new Vector3f(0.0F, 1.0F / 16.0F, 0.0F));
        assertEquals(0.5625F, baseBottom.x(), EPSILON);
        assertEquals(0.0F, baseBottom.y(), EPSILON);
        assertEquals(0.4375F, baseBottom.z(), EPSILON);

        PoseStack liquidPose = new PoseStack();
        ChaliceRenderer.applySourcePose(liquidPose);
        model.liquid().translateAndRotate(liquidPose);
        Vector3f liquid = liquidPose.last().pose().transformPosition(new Vector3f());
        assertEquals(0.625F, liquid.x(), EPSILON);
        assertEquals(0.3125F, liquid.y(), EPSILON);
        assertEquals(0.375F, liquid.z(), EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
