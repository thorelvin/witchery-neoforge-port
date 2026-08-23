package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.KettleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class KettleRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void directionalWorldRendererUsesTheExactSourceMatrixOrder() {
        for (float yaw : new float[] {0.0F, 90.0F, 180.0F, 270.0F}) {
            PoseStack actual = new PoseStack();
            KettleRenderer.applySourcePose(actual, yaw);

            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.translate(0.0F, -1.0F, 0.0F);
            expected.mulPose(Axis.YP.rotationDegrees(yaw));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void sourceReachableLiquidFramesCycleWithoutEnteringUnusedFrames() {
        assertEquals(0, KettleRenderer.liquidFrame(0));
        assertEquals(0, KettleRenderer.liquidFrame(19));
        assertEquals(1, KettleRenderer.liquidFrame(20));
        assertEquals(2, KettleRenderer.liquidFrame(40));
        assertEquals(3, KettleRenderer.liquidFrame(60));
        assertEquals(0, KettleRenderer.liquidFrame(80));
    }

    @Test
    void fullLiquidMatchesSourceHeightAndLowerLevelsStayInsideTheBowl() {
        ModelPart root = KettleModel.createBodyLayer().bakeRoot();
        KettleModel model = new KettleModel(root);

        assertLiquidHeight(model, 1, 0.125F);
        assertLiquidHeight(model, 2, 0.1875F);
        assertLiquidHeight(model, 3, 0.25F);
    }

    private static void assertLiquidHeight(KettleModel model, int level, float expectedY) {
        PoseStack pose = new PoseStack();
        KettleRenderer.applySourcePose(pose, 0.0F);
        pose.translate(0.0F, KettleRenderer.liquidYOffset(level), 0.0F);
        model.liquid(0).translateAndRotate(pose);
        Matrix4f matrix = pose.last().pose();
        Vector3f first = matrix.transformPosition(new Vector3f());
        Vector3f opposite = matrix.transformPosition(new Vector3f(0.5F, 0.0F, 0.5F));

        assertEquals(0.75F, first.x(), EPSILON);
        assertEquals(expectedY, first.y(), EPSILON);
        assertEquals(0.25F, first.z(), EPSILON);
        assertEquals(0.25F, opposite.x(), EPSILON);
        assertEquals(expectedY, opposite.y(), EPSILON);
        assertEquals(0.75F, opposite.z(), EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
