package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import net.minecraft.world.item.ItemDisplayContext;
import org.junit.jupiter.api.Test;

class DeathHandSpecialRendererTest {
    @Test
    void choosesGeometryByViewInsteadOfHeldSide() {
        assertTrue(WitcherySpecialItemRenderers.DeathHandSpecialRenderer
                .usesFirstPersonGeometry(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND));
        assertTrue(WitcherySpecialItemRenderers.DeathHandSpecialRenderer
                .usesFirstPersonGeometry(ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
        assertFalse(WitcherySpecialItemRenderers.DeathHandSpecialRenderer
                .usesFirstPersonGeometry(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND));
        assertFalse(WitcherySpecialItemRenderers.DeathHandSpecialRenderer
                .usesFirstPersonGeometry(ItemDisplayContext.THIRD_PERSON_LEFT_HAND));
    }

    @Test
    void deployedThirdPersonOffsetMatchesSourceAndMirrorsForOffhand() {
        assertEquals(0.19F, WitcherySpecialItemRenderers.DeathHandSpecialRenderer
                .thirdPersonDeployedXOffset(true));
        assertEquals(-0.19F, WitcherySpecialItemRenderers.DeathHandSpecialRenderer
                .thirdPersonDeployedXOffset(false));
    }

    @Test
    void firstPersonRestPoseReproducesTheLegacyPipeline() {
        PoseStack actual = new PoseStack();
        WitcherySpecialItemRenderers.DeathHandSpecialRenderer.applyFirstPersonPose(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.94F, 0.13F, 0.0F);
        expected.mulPose(Axis.YP.rotationDegrees(45.0F));
        expected.scale(0.4F, 0.4F, 0.4F);
        applyLegacyEquippedItemPose(expected);
        applySourcePose(expected);
        expected.scale(1.7F, 1.7F, 1.7F);

        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void sourcePoseRetainsTheOriginalModelRendererTransform() {
        PoseStack poseStack = new PoseStack();
        WitcherySpecialItemRenderers.DeathHandSpecialRenderer.applySourcePose(poseStack);

        PoseStack expected = new PoseStack();
        applySourcePose(expected);
        assertMatrixEquals(expected.last().pose(), poseStack.last().pose());
    }

    @Test
    void thirdPersonAdapterReplacesTheModernTailWithTheLegacyFull3dTail() {
        PoseStack actual = new PoseStack();
        applyModernThirdPersonTail(actual, true);
        WitcherySpecialItemRenderers.DeathHandSpecialRenderer.applyThirdPersonPose(actual, true);

        PoseStack expected = new PoseStack();
        expected.translate(-0.93F, -0.64684546F, 0.01979959F);
        applyLegacyThirdPersonTail(expected, true);
        applyLegacyEquippedItemPose(expected);
        applySourcePose(expected);

        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static void applyModernThirdPersonTail(PoseStack poseStack, boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(side / 16.0F, 0.125F, -0.625F);
    }

    private static void applyLegacyThirdPersonTail(PoseStack poseStack, boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;
        poseStack.translate(-side / 16.0F, 0.625F, 0.0625F);
        poseStack.scale(0.625F, -0.625F, 0.625F);
        poseStack.mulPose(Axis.XN.rotationDegrees(100.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
    }

    private static void applyLegacyEquippedItemPose(PoseStack poseStack) {
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
    }

    private static void applySourcePose(PoseStack poseStack) {
        poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(51.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(81.0F));
        poseStack.translate(0.125F, 0.12F, -0.85F);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), 0.00001F,
                        "matrix[" + column + "][" + row + "]");
            }
        }
    }
}
