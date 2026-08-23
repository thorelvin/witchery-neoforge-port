package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WolfHeadRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void floorAndWallPosesUseTheExactSourceMatrixOrder() {
        assertPose(Direction.UP, 67.5F, 0.5F, 0.0F, 0.5F);
        assertPose(Direction.NORTH, 0.0F, 0.5F, 0.25F, 0.74F);
        assertPose(Direction.SOUTH, 180.0F, 0.5F, 0.25F, 0.26F);
        assertPose(Direction.WEST, 270.0F, 0.74F, 0.25F, 0.5F);
        assertPose(Direction.EAST, 90.0F, 0.26F, 0.25F, 0.5F);
    }

    @Test
    void sourceHeadCentersRemainOnTheirExactAttachmentPlanes() {
        assertHeadCenter(Direction.UP, 0.0F, 0.5F, 3.0F / 16.0F, 0.5F + 0.5F / 16.0F);
        assertHeadCenter(Direction.NORTH, 0.0F, 0.5F, 7.0F / 16.0F, 0.74F + 0.5F / 16.0F);
        assertHeadCenter(Direction.SOUTH, 180.0F, 0.5F, 7.0F / 16.0F, 0.26F - 0.5F / 16.0F);
        assertHeadCenter(Direction.WEST, 270.0F, 0.74F + 0.5F / 16.0F, 7.0F / 16.0F, 0.5F);
        assertHeadCenter(Direction.EAST, 90.0F, 0.26F - 0.5F / 16.0F, 7.0F / 16.0F, 0.5F);
    }

    private static void assertPose(Direction facing, float yaw, float x, float y, float z) {
        PoseStack actual = new PoseStack();
        WolfHeadRenderer.applySourcePose(actual, facing, yaw);

        PoseStack expected = new PoseStack();
        expected.translate(x, y, z);
        expected.scale(-1.0F, -1.0F, 1.0F);
        expected.mulPose(Axis.YP.rotationDegrees(yaw));
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static void assertHeadCenter(Direction facing, float yaw, float x, float y, float z) {
        PoseStack pose = new PoseStack();
        WolfHeadRenderer.applySourcePose(pose, facing, yaw);
        Vector3f center = pose.last().pose().transformPosition(new Vector3f(0.0F, -3.0F / 16.0F, 0.5F / 16.0F));
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
