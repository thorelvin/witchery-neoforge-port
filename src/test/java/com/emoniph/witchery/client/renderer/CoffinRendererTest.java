package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.CoffinModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CoffinRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyFacingUsesTheExactSourceMatrixOrder() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack actual = new PoseStack();
            CoffinRenderer.applySourcePose(actual, facing);

            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.translate(0.0F, -0.1F, 0.0F);
            expected.mulPose(Axis.YP.rotationDegrees(CoffinRenderer.sourceRotation(facing)));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void southFacingSideWallsMeetExactlyAtTheTwoBlockSeam() {
        ModelPart root = CoffinModel.createBodyLayer().bakeRoot();
        CoffinModel model = new CoffinModel(root);
        ModelPart side = root.getChild("side_left");

        model.setupSourcePose(BedPart.FOOT, 0.0F);
        PoseStack footPose = new PoseStack();
        CoffinRenderer.applySourcePose(footPose, Direction.SOUTH);
        side.translateAndRotate(footPose);
        assertEquals(0.0625F, transformZ(footPose, -0.5F), EPSILON);
        assertEquals(1.0F, transformZ(footPose, 0.4375F), EPSILON);

        model.setupSourcePose(BedPart.HEAD, 0.0F);
        PoseStack headPose = new PoseStack();
        headPose.translate(0.0F, 0.0F, 1.0F);
        CoffinRenderer.applySourcePose(headPose, Direction.SOUTH);
        side.translateAndRotate(headPose);
        assertEquals(1.0F, transformZ(headPose, -0.5F), EPSILON);
        assertEquals(1.9375F, transformZ(headPose, 0.4375F), EPSILON);
    }

    private static float transformZ(PoseStack poseStack, float localZ) {
        return poseStack.last().pose().transformPosition(new Vector3f(0.0F, 0.0F, localZ)).z();
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
