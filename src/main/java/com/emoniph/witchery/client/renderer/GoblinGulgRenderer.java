package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GoblinGulgModel;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class GoblinGulgRenderer extends MobRenderer<GoblinGulgEntity, GoblinGulgModel.GulgRenderState, GoblinGulgModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/gulg.png");

    public GoblinGulgRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinGulgModel(ctx.bakeLayer(GoblinGulgModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override public GoblinGulgModel.GulgRenderState createRenderState() {
        return new GoblinGulgModel.GulgRenderState();
    }

    @Override
    public void extractRenderState(GoblinGulgEntity entity, GoblinGulgModel.GulgRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.crouching = entity.isCrouching();
        state.holdingRightItem = !entity.getMainHandItem().isEmpty();
        state.holdingLeftItem = false;
        state.aimedBow = false;
        state.swingProgress = entity.getAttackAnim(partialTick);
        state.attackTimer = entity.getAttackTimer();
    }

    @Override
    protected void scale(GoblinGulgModel.GulgRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y - (state.crouching ? 0.125F : 0.0F), 0.0F);
    }

    @Override public Identifier getTextureLocation(GoblinGulgModel.GulgRenderState state) { return TEXTURE; }
}
