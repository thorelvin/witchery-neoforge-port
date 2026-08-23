package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GoblinMogModel;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class GoblinMogRenderer extends MobRenderer<GoblinMogEntity, GoblinMogModel.MogRenderState, GoblinMogModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mog.png");

    public GoblinMogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinMogModel(ctx.bakeLayer(GoblinMogModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override public GoblinMogModel.MogRenderState createRenderState() {
        return new GoblinMogModel.MogRenderState();
    }

    @Override
    public void extractRenderState(GoblinMogEntity entity, GoblinMogModel.MogRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.crouching = entity.isCrouching();
        state.holdingRightItem = !entity.getMainHandItem().isEmpty();
        state.holdingLeftItem = false;
        state.aimedBow = false;
        state.swingProgress = entity.getAttackAnim(partialTick);
    }

    @Override
    protected void scale(GoblinMogModel.MogRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y - (state.crouching ? 0.125F : 0.0F), 0.0F);
    }

    @Override public Identifier getTextureLocation(GoblinMogModel.MogRenderState state) { return TEXTURE; }
}
