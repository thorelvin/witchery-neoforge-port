package com.emoniph.witchery.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** Re-renders the parent model with the source Colorful dye tint. */
public final class ColorfulLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {
    public static final ContextKey<Integer> TINT =
            new ContextKey<>(Identifier.fromNamespaceAndPath("witchery", "colorful_tint"));
    private final LivingEntityRenderer<?, S, M> parent;

    public ColorfulLayer(LivingEntityRenderer<?, S, M> parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       S state, float yRot, float xRot) {
        Integer tint = state.getRenderData(TINT);
        if (tint == null || state.isInvisibleToPlayer) {
            return;
        }
        collector.submitModel(this.getParentModel(), state, poseStack,
                RenderTypes.entityCutoutNoCull(this.parent.getTextureLocation(state)), light,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), 0xFF000000 | tint,
                null, state.outlineColor, null);
    }
}
