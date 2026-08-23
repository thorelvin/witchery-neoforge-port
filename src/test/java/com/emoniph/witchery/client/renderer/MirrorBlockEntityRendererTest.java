package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.MirrorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MirrorBlockEntityRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void bothHalvesAndEveryFacingUseTheExactSourceTesrMatrixOrder() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean top : new boolean[]{false, true}) {
                PoseStack actual = new PoseStack();
                MirrorBlockEntityRenderer.applySourcePose(actual, facing, top);
                assertMatrixEquals(sourcePose(facing, top).last().pose(), actual.last().pose());
            }
        }
    }

    @Test
    void glassCentersOccupyTheExactSourceWallPlanesAndHalfHeights() {
        MirrorModel model = new MirrorModel(MirrorModel.createBodyLayer().bakeRoot());
        assertBackCenter(model, Direction.NORTH, false, 0.5F, 0.53125F, 0.97125F);
        assertBackCenter(model, Direction.NORTH, true, 0.5F, 1.46875F, 0.97125F);
        assertBackCenter(model, Direction.SOUTH, false, 0.5F, 0.53125F, 0.02875F);
        assertBackCenter(model, Direction.WEST, false, 0.97125F, 0.53125F, 0.5F);
        assertBackCenter(model, Direction.EAST, false, 0.02875F, 0.53125F, 0.5F);
    }

    private static void assertBackCenter(MirrorModel model, Direction facing, boolean top,
                                         float x, float y, float z) {
        ModelPart back = model.backMiddle();
        PoseStack pose = new PoseStack();
        if (top) {
            pose.translate(0.0F, 1.0F, 0.0F);
        }
        MirrorBlockEntityRenderer.applySourcePose(pose, facing, top);
        back.translateAndRotate(pose);
        Vector3f center = pose.last().pose().transformPosition(
                new Vector3f(0.0F, 0.5F / 16.0F, -0.5F / 16.0F));
        assertEquals(x, center.x(), EPSILON, facing + " x");
        assertEquals(y, center.y(), EPSILON, facing + " y");
        assertEquals(z, center.z(), EPSILON, facing + " z");
    }

    private static PoseStack sourcePose(Direction facing, boolean top) {
        PoseStack pose = new PoseStack();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(Axis.YP.rotationDegrees(MirrorBlockEntityRenderer.sourceRotation(facing)));
        if (!top) {
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        pose.translate(0.0F, 0.1F, 0.42F);
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
