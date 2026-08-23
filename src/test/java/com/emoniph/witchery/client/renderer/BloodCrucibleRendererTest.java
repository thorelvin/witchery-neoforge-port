package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.BloodCrucibleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BloodCrucibleRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void bodyUsesTheExactSourceTesrMatrix() {
        PoseStack actual = new PoseStack();
        BloodCrucibleRenderer.applySourcePose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.translate(0.0F, -0.5F, 0.0F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void halfFullLiquidOccupiesTheSourceWorldSpaceAnchors() {
        ModelPart bodyRoot = BloodCrucibleModel.createBodyLayer().bakeRoot();
        ModelPart liquidRoot = BloodCrucibleModel.createLiquidLayer().bakeRoot();
        BloodCrucibleModel model = new BloodCrucibleModel(bodyRoot, liquidRoot);
        model.setBloodPercent(0.5F);

        PoseStack poseStack = new PoseStack();
        BloodCrucibleRenderer.applySourcePose(poseStack);
        model.blood().translateAndRotate(poseStack);
        Matrix4f matrix = poseStack.last().pose();

        Vector3f first = matrix.transformPosition(new Vector3f(0.0F, 0.0F, 0.0F));
        Vector3f opposite = matrix.transformPosition(new Vector3f(1.0F, 0.0F, 1.0F));
        assertEquals(0.6875F, first.x, EPSILON);
        assertEquals(0.2F, first.y, EPSILON);
        assertEquals(0.3125F, first.z, EPSILON);
        assertEquals(0.3125F, opposite.x, EPSILON);
        assertEquals(0.2F, opposite.y, EPSILON);
        assertEquals(0.6875F, opposite.z, EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON,
                        "matrix[" + column + "][" + row + "]");
            }
        }
    }
}
