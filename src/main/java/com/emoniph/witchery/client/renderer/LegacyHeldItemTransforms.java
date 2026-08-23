package com.emoniph.witchery.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Replaces the modern held-item tail with the 1.7.10 full-3D/Forge item-renderer pipeline. */
final class LegacyHeldItemTransforms {
    private static final float THIRD_PERSON_OUTWARD_ALIGNMENT = 0.93F;

    private LegacyHeldItemTransforms() {
    }

    static void beginFirstPerson(PoseStack poseStack) {
        poseStack.translate(0.94F, 0.13F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        applyLegacyEquippedItemPose(poseStack);
    }

    static void finishFirstPerson(PoseStack poseStack) {
        poseStack.scale(1.7F, 1.7F, 1.7F);
    }

    static void beginThirdPerson(PoseStack poseStack, boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;

        poseStack.translate(-side / 16.0F, -0.125F, 0.625F);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.translate(side * -THIRD_PERSON_OUTWARD_ALIGNMENT, -0.64684546F, 0.01979959F);
        poseStack.translate(-side / 16.0F, 0.625F, 0.0625F);
        poseStack.scale(0.625F, -0.625F, 0.625F);
        poseStack.mulPose(Axis.XN.rotationDegrees(100.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        applyLegacyEquippedItemPose(poseStack);
    }

    static void applyLegacyEquippedItemPose(PoseStack poseStack) {
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
    }
}
