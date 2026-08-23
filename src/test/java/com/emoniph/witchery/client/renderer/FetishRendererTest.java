package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.FetishModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FetishRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void everyFacingUsesTheExactSourceMatrixAndMetadataRotation() {
        assertEquals(0.0F, FetishRenderer.sourceRotation(Direction.NORTH));
        assertEquals(90.0F, FetishRenderer.sourceRotation(Direction.EAST));
        assertEquals(180.0F, FetishRenderer.sourceRotation(Direction.SOUTH));
        assertEquals(270.0F, FetishRenderer.sourceRotation(Direction.WEST));

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack actual = new PoseStack();
            FetishRenderer.applySourcePose(actual, facing);

            PoseStack expected = new PoseStack();
            expected.translate(0.5F, 0.5F, 0.5F);
            expected.mulPose(Axis.ZP.rotationDegrees(180.0F));
            expected.translate(0.0F, -1.0F, 0.0F);
            expected.mulPose(Axis.YP.rotationDegrees(FetishRenderer.sourceRotation(facing)));
            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void scarecrowPoleTouchesTheSourceGroundPlaneForEveryFacing() {
        ModelPart pole = FetishModel.createBodyLayer().bakeRoot().getChild("pole_vertical");
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack pose = new PoseStack();
            FetishRenderer.applySourcePose(pose, facing);
            pole.translateAndRotate(pose);
            Vector3f bottom = pose.last().pose().transformPosition(new Vector3f(0.0F, 15.0F / 16.0F, 0.0F));
            assertEquals(0.0F, bottom.y(), EPSILON, facing.toString());
        }
    }

    @Test
    void sourceFleecePaletteAndFallbackColorArePreserved() {
        assertEquals(ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F),
                FetishRenderer.sourceTintColor(0, 1.0F));
        assertEquals(ARGB.colorFromFloat(0.7F, 0.6F, 0.2F, 0.2F),
                FetishRenderer.sourceTintColor(14, 0.7F));
        assertEquals(ARGB.colorFromFloat(1.0F, 0.3F, 0.5F, 0.6F),
                FetishRenderer.sourceTintColor(-1, 1.0F));
        assertEquals(FetishRenderer.sourceTintColor(9, 1.0F),
                FetishRenderer.sourceTintColor(16, 1.0F));
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON);
            }
        }
    }
}
