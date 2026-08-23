package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.FlameImpModel;
import com.emoniph.witchery.client.model.FlameImpWingModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

class FlameImpWingLayer extends RenderLayer<FlameImpModel.FlameImpRenderState, FlameImpModel> {
    private final FlameImpWingModel wings;

    FlameImpWingLayer(FlameImpRenderer parent,
                      net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        super(parent);
        wings = new FlameImpWingModel(context.bakeLayer(FlameImpWingModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       FlameImpModel.FlameImpRenderState state, float yRot, float xRot) {
        if (!state.isInvisible) {
            collector.submitModel(wings, state, poseStack,
                    RenderTypes.entityCutoutNoCull(FlameImpRenderer.TEXTURE), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), -1,
                    null, state.outlineColor, null);
        }
    }
}
