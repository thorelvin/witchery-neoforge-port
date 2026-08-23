package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.GarlicGarlandModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GarlicGarlandRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyFacingUsesTheExactSourceTesrMatrixOrder() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack actual = new PoseStack();
            GarlicGarlandRenderer.applySourcePose(actual, facing);
            assertMatrixEquals(sourcePose(facing).last().pose(), actual.last().pose());
        }
    }

    @Test
    void centerGarlicHangsAtTheExactSourceWallPlaneAndHeight() {
        GarlicGarlandModel model = new GarlicGarlandModel(
                GarlicGarlandModel.createBodyLayer().bakeRoot());
        assertBottomBulbCenter(model, Direction.NORTH, 0.5F, 0.9015625F, 0.9575F);
        assertBottomBulbCenter(model, Direction.SOUTH, 0.5F, 0.9015625F, 0.0425F);
        assertBottomBulbCenter(model, Direction.WEST, 0.9575F, 0.9015625F, 0.5F);
        assertBottomBulbCenter(model, Direction.EAST, 0.0425F, 0.9015625F, 0.5F);
    }

    @Test
    void cordRotationOccursAroundTheExactSourcePivot() {
        ModelPart string = GarlicGarlandModel.createBodyLayer().bakeRoot().getChild("string_1");
        PoseStack pose = new PoseStack();
        GarlicGarlandRenderer.applySourcePose(pose, Direction.NORTH);
        string.translateAndRotate(pose);
        Vector3f center = pose.last().pose().transformPosition(new Vector3f(2.0F / 16.0F, 0.0F, 0.0F));

        float sourceX = -5.4F + 2.0F * (float) Math.cos(GarlicGarlandModel.STRING_ANGLE);
        float sourceY = -0.3F + 2.0F * (float) Math.sin(GarlicGarlandModel.STRING_ANGLE);
        assertEquals(0.5F - sourceX / 16.0F, center.x(), EPSILON);
        assertEquals(1.0F - sourceY / 16.0F, center.y(), EPSILON);
        assertEquals(0.9575F, center.z(), EPSILON);
    }

    private static void assertBottomBulbCenter(GarlicGarlandModel model, Direction facing,
                                                float x, float y, float z) {
        ModelPart garlic = model.root().getChild("garlic_c");
        ModelPart bulb = garlic.getChild("garlic_4");
        PoseStack pose = new PoseStack();
        GarlicGarlandRenderer.applySourcePose(pose, facing);
        garlic.translateAndRotate(pose);
        bulb.translateAndRotate(pose);
        Vector3f center = pose.last().pose().transformPosition(
                new Vector3f(0.0F, 7.5F / 16.0F, 0.0F));
        assertEquals(x, center.x(), EPSILON, facing + " x");
        assertEquals(y, center.y(), EPSILON, facing + " y");
        assertEquals(z, center.z(), EPSILON, facing + " z");
    }

    private static PoseStack sourcePose(Direction facing) {
        PoseStack pose = new PoseStack();
        pose.translate(0.5F, 0.9F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(Axis.YP.rotationDegrees(GarlicGarlandRenderer.sourceRotation(facing)));
        pose.translate(0.0F, -0.1F, 0.02F);
        return pose;
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
