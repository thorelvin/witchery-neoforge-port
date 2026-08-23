package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.client.model.DreamCatcherModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DreamCatcherRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyFacingUsesTheExactSourceMatrixOrder() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack actual = new PoseStack();
            DreamCatcherRenderer.applySourcePose(actual, facing);
            assertMatrixEquals(sourcePose(facing).last().pose(), actual.last().pose());
        }
    }

    @Test
    void netCenterSitsOnTheExactSourceWallPlaneForEveryFacing() {
        DreamCatcherModel model = new DreamCatcherModel(DreamCatcherModel.createBodyLayer().bakeRoot());
        ModelPart net = model.net(DreamWeaveType.FLEET_FOOT);

        assertNetCenter(net, Direction.NORTH, 0.50F, 0.625F, 0.99F);
        assertNetCenter(net, Direction.SOUTH, 0.50F, 0.625F, 0.01F);
        assertNetCenter(net, Direction.WEST, 0.99F, 0.625F, 0.50F);
        assertNetCenter(net, Direction.EAST, 0.01F, 0.625F, 0.50F);
    }

    private static PoseStack sourcePose(Direction facing) {
        PoseStack pose = new PoseStack();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.translate(0.0F, -1.0F, 0.0F);
        switch (facing) {
            case NORTH -> pose.translate(0.0F, 0.0F, -0.01F);
            case SOUTH -> pose.translate(0.0F, 0.0F, 0.01F);
            case WEST -> pose.translate(0.01F, 0.0F, 0.0F);
            case EAST -> pose.translate(-0.01F, 0.0F, 0.0F);
            default -> throw new IllegalArgumentException();
        }
        pose.mulPose(Axis.YP.rotationDegrees(DreamCatcherRenderer.sourceRotation(facing)));
        return pose;
    }

    private static void assertNetCenter(ModelPart net, Direction facing, float x, float y, float z) {
        PoseStack pose = new PoseStack();
        DreamCatcherRenderer.applySourcePose(pose, facing);
        net.translateAndRotate(pose);
        Vector3f center = pose.last().pose().transformPosition(new Vector3f(0.1875F, 0.1875F, 0.0F));
        assertEquals(x, center.x(), EPSILON, facing + " x");
        assertEquals(y, center.y(), EPSILON, facing + " y");
        assertEquals(z, center.z(), EPSILON, facing + " z");
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
