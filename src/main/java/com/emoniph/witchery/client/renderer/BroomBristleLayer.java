package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BroomModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

class BroomBristleLayer extends RenderLayer<BroomRenderer.BroomRenderState, BroomModel> {

    private final BroomModel model;

    BroomBristleLayer(BroomRenderer parent, net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) {
        super(parent);
        this.model = new BroomModel(ctx.bakeLayer(BroomModel.LAYER_LOCATION));
        this.model.setHandleVisible(false);
        this.model.setBristlesVisible(true);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       BroomRenderer.BroomRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        collector.submitModel(this.model, state, poseStack, RenderTypes.entityCutoutNoCull(BroomRenderer.TEXTURE),
                light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), BroomRenderer.brushColor(state.brushColor),
                null, state.outlineColor, null);
    }
}
