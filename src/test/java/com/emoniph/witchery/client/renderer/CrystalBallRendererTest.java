package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.CrystalBallModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CrystalBallRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void worldRendererUsesTheExactSourceMatrixOrder() {
        PoseStack actual = new PoseStack();
        CrystalBallRenderer.applySourcePose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(0.0F, -1.0F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void innerGlobeUsesTheSourceTriangleWave() {
        assertEquals(0.20F, CrystalBallRenderer.sourceBrightness(0), EPSILON);
        assertEquals(0.99F, CrystalBallRenderer.sourceBrightness(79), EPSILON);
        assertEquals(1.00F, CrystalBallRenderer.sourceBrightness(80), EPSILON);
        assertEquals(0.21F, CrystalBallRenderer.sourceBrightness(159), EPSILON);
        assertEquals(0.20F, CrystalBallRenderer.sourceBrightness(160), EPSILON);
    }

    @Test
    void outerGlobeOccupiesTheExactSourceWorldVolume() {
        ModelPart outer = CrystalBallModel.createBodyLayer().bakeRoot().getChild("globe_outer");
        PoseStack pose = new PoseStack();
        CrystalBallRenderer.applySourcePose(pose);
        outer.translateAndRotate(pose);

        Matrix4f matrix = pose.last().pose();
        assertVector(matrix.transformPosition(new Vector3f(0.0F, 0.0F, 0.0F)),
                0.6875F, 0.5625F, 0.3125F);
        assertVector(matrix.transformPosition(new Vector3f(0.375F, 0.375F, 0.375F)),
                0.3125F, 0.1875F, 0.6875F);
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x(), EPSILON);
        assertEquals(y, actual.y(), EPSILON);
        assertEquals(z, actual.z(), EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
