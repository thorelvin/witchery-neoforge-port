package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.RavenModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RavenEyesLayer extends RenderLayer<LivingEntityRenderState, RavenModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/raven_eyes.png");

    public RavenEyesLayer(RenderLayerParent<LivingEntityRenderState, RavenModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LivingEntityRenderState state, float f1, float f2) {
        collector.submitModelPart(this.getParentModel().root(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
    }
}
