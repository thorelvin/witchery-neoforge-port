package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emoniph.witchery.client.model.SilverVatModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SilverVatRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void worldRendererUsesTheExactSourceMatrixOrder() {
        PoseStack actual = new PoseStack();
        SilverVatRenderer.applySourcePose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(0.0F, -1.0F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void decomposedLiquidPlaneMatchesTheSourceTessellatorFootprint() {
        ModelPart liquid = SilverVatModel.createLiquidLayer().bakeRoot().getChild("liquid");
        PoseStack pose = new PoseStack();
        SilverVatRenderer.applySourcePose(pose);
        SilverVatRenderer.applyLiquidScale(pose);
        liquid.translateAndRotate(pose);

        Matrix4f matrix = pose.last().pose();
        Vector3f northwest = matrix.transformPosition(new Vector3f(-0.5F, 0.0F, -0.5F));
        Vector3f southeast = matrix.transformPosition(new Vector3f(0.5F, 0.0F, 0.5F));
        assertVector(northwest, 0.875F, 0.3F, 0.125F);
        assertVector(southeast, 0.125F, 0.3F, 0.875F);
    }

    @Test
    void sourceSpoutsAppearForItemViewsOrAdjacentBlockEntitiesOnly() {
        assertTrue(SilverVatRenderer.rendersSpout(true, false));
        assertTrue(SilverVatRenderer.rendersSpout(false, true));
        assertFalse(SilverVatRenderer.rendersSpout(false, false));
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
