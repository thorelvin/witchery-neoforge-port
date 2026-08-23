package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BabaYagaModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Restores the source renderer's nose-anchored held item. */
final class BabaYagaHeldItemLayer extends RenderLayer<BabaYagaModel.BabaYagaRenderState, BabaYagaModel> {
    BabaYagaHeldItemLayer(BabaYagaRenderer renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       BabaYagaModel.BabaYagaRenderState state, float yRot, float xRot) {
        if (state.heldItem.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        getParentModel().translateToSourceHeldItemAnchor(poseStack);
        applyCommonSourceTransform(poseStack);
        state.heldItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    static void applyCommonSourceTransform(PoseStack poseStack) {
        poseStack.translate(-0.0625F, 0.53125F, 0.21875F);
        poseStack.translate(0.25F, 0.1875F, -0.1875F);
        poseStack.scale(0.375F, 0.375F, 0.375F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(60.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(40.0F));
    }
}
