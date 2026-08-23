package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.WolfAltarModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class WolfAltarRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyWorldFacingUsesTheExactSourceMetadataRotationAndMatrixOrder() {
        assertEquals(0.0F, WolfAltarRenderer.sourceRotation(Direction.NORTH));
        assertEquals(90.0F, WolfAltarRenderer.sourceRotation(Direction.EAST));
        assertEquals(180.0F, WolfAltarRenderer.sourceRotation(Direction.SOUTH));
        assertEquals(270.0F, WolfAltarRenderer.sourceRotation(Direction.WEST));

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack actual = new PoseStack();
            WolfAltarRenderer.applySourceWorldPose(actual, facing);

            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.translate(0.0F, -1.0F, 0.0F);
            expected.mulPose(Axis.YP.rotationDegrees(WolfAltarRenderer.sourceRotation(facing)));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void sourceInventoryTesrPoseIsPreserved() {
        PoseStack actual = new PoseStack();
        WolfAltarRenderer.applySourceItemPose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.5F, 0.5F, 0.5F);
        expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
        expected.translate(0.0F, -1.0F, 0.0F);
        expected.scale(0.6F, 0.6F, 0.6F);
        expected.mulPose(Axis.YP.rotationDegrees(180.0F));
        expected.translate(0.0F, 1.5F, 0.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void subtreeScaleIsAppliedAboutTheExactSourcePivot() {
        PoseStack actual = new PoseStack();
        WolfAltarModel.scaleAboutPivot(actual, -5.0F, 14.0F, 3.5F, 0.5F);

        PoseStack expected = new PoseStack();
        expected.translate(-5.0F / 16.0F, 14.0F / 16.0F, 3.5F / 16.0F);
        expected.scale(0.5F, 0.5F, 0.5F);
        expected.translate(5.0F / 16.0F, -14.0F / 16.0F, -3.5F / 16.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
