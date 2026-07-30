package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GrotesqueModel;
import com.emoniph.witchery.effect.GrotesqueMobEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/**
 * Renders the source demon-head overlay for the composed Grotesque effect and fixed brew aura.
 */
public final class GrotesqueLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {
    public static final ContextKey<OverlayStatus> OVERLAY_STATUS =
            new ContextKey<>(Identifier.fromNamespaceAndPath("witchery", "grotesque_overlay"));

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/demon.png");
    private static final int INVISIBLE_TINT = 0x26FFFFFF;

    private final GrotesqueModel model;

    public GrotesqueLayer(RenderLayerParent<S, M> parent, EntityRendererProvider.Context context) {
        super(parent);
        this.model = new GrotesqueModel(context.bakeLayer(GrotesqueModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       S state, float yRot, float xRot) {
        OverlayStatus status = state.getRenderData(OVERLAY_STATUS);
        if (status == null
                || !GrotesqueMobEffect.isOverlayActive(status.effectActive(), status.fixedAuraActive())) {
            return;
        }

        // The fixed player style was hidden with its owner; the potion overlay retained
        // the source renderer's 15% alpha visibility when the local viewer could see it.
        if (state.isInvisibleToPlayer || (state.isInvisible && !status.effectActive())) {
            return;
        }
        RenderType renderType = state.isInvisible
                ? RenderTypes.entityTranslucent(TEXTURE)
                : RenderTypes.entityCutoutNoCull(TEXTURE);
        int tint = state.isInvisible ? INVISIBLE_TINT : -1;

        poseStack.pushPose();
        if (status.effectActive()) {
            poseStack.scale(1.01F, 1.01F, 1.01F);
        }
        poseStack.translate(0.0F, 0.735F, 0.0F);
        poseStack.scale(1.3F, 1.3F, 1.3F);
        this.model.setupAnim(state);
        collector.submitModelPart(this.model.root(), poseStack, renderType, light,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), null, tint, null);
        poseStack.popPose();
    }

    public record OverlayStatus(boolean effectActive, boolean fixedAuraActive) {}
}
