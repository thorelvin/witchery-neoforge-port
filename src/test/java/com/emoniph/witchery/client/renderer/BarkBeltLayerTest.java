package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.BarkGrowthModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class BarkBeltLayerTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void chargeGroupsAdvanceOnlyThroughClothingSlotsThatActuallyExist() {
        assertEquals(EnumSet.noneOf(BarkGrowthModel.Group.class),
                BarkBeltLayer.groupsFor(0, true, true, true));
        assertEquals(EnumSet.of(BarkGrowthModel.Group.BODY),
                BarkBeltLayer.groupsFor(1, true, true, true));
        assertEquals(EnumSet.of(BarkGrowthModel.Group.BODY, BarkGrowthModel.Group.SPIKES),
                BarkBeltLayer.groupsFor(2, true, true, true));
        assertEquals(EnumSet.of(BarkGrowthModel.Group.BODY, BarkGrowthModel.Group.SPIKES,
                        BarkGrowthModel.Group.RIGHT_ARM),
                BarkBeltLayer.groupsFor(3, false, true, false));
        assertEquals(EnumSet.of(BarkGrowthModel.Group.BODY, BarkGrowthModel.Group.SPIKES,
                        BarkGrowthModel.Group.RIGHT_HEAD_SHOULDER,
                        BarkGrowthModel.Group.LEFT_HEAD_SHOULDER),
                BarkBeltLayer.groupsFor(4, false, false, true));
        assertEquals(EnumSet.allOf(BarkGrowthModel.Group.class),
                BarkBeltLayer.groupsFor(8, true, true, true));
    }

    @Test
    void rotatedPartsUseTheSourcePivotRotationInversePivotMatrix() {
        ModelPart part = emptyPart(5.0F, 2.0F, -1.0F, 0.2F, -0.3F, 0.4F);
        PoseStack actual = new PoseStack();
        BarkBeltLayer.applyLegacyAnchor(actual, part, false, false);

        PoseStack expected = new PoseStack();
        expected.translate(5.0F / 16.0F, 2.0F / 16.0F, -1.0F / 16.0F);
        expected.mulPose(Axis.ZP.rotation(0.4F));
        expected.mulPose(Axis.YP.rotation(-0.3F));
        expected.mulPose(Axis.XP.rotation(0.2F));
        expected.translate(-5.0F / 16.0F, -2.0F / 16.0F, 1.0F / 16.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void unrotatedNonLegPartsRetainTheSourceDirectPivotBranch() {
        ModelPart part = emptyPart(-5.0F, 2.0F, 3.0F, 0.0F, 0.0F, 0.0F);
        PoseStack actual = new PoseStack();
        BarkBeltLayer.applyLegacyAnchor(actual, part, false, false);

        PoseStack expected = new PoseStack();
        expected.translate(-5.0F / 16.0F, 2.0F / 16.0F, 3.0F / 16.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void crouchingLegsUseTheSourceAbsolutePieceRebase() {
        ModelPart part = emptyPart(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        PoseStack actual = new PoseStack();
        BarkBeltLayer.applyLegacyAnchor(actual, part, true, true);

        PoseStack expected = new PoseStack();
        expected.translate(-1.9F / 16.0F, 12.0F / 16.0F, 0.0F);
        expected.translate(1.9F / 16.0F, -12.0F / 16.0F, 0.0F);
        expected.translate(0.0F, -3.0F / 16.0F, 4.0F / 16.0F);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static ModelPart emptyPart(float x, float y, float z,
                                       float xRot, float yRot, float zRot) {
        ModelPart part = new ModelPart(List.of(), Map.of());
        part.setPos(x, y, z);
        part.setRotation(xRot, yRot, zRot);
        return part;
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
