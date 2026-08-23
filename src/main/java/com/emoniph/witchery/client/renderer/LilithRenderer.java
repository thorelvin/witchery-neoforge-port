package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LilithModel;
import com.emoniph.witchery.entity.LilithEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class LilithRenderer extends MobRenderer<LilithEntity, LilithModel.LilithRenderState, LilithModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final int SOURCE_LIGHT = LightTexture.FULL_BRIGHT;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/lilith.png");

    public LilithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LilithModel(ctx.bakeLayer(LilithModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override
    public LilithModel.LilithRenderState createRenderState() {
        return new LilithModel.LilithRenderState();
    }

    @Override
    public void extractRenderState(LilithEntity entity, LilithModel.LilithRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTimer = entity.getAttackTimer();
        state.isFriendly  = entity.isFriendly();
        state.riding = entity.isPassenger();
        state.lightCoords = SOURCE_LIGHT;
    }

    @Override
    protected void scale(LilithModel.LilithRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(LilithModel.LilithRenderState state) {
        return TEXTURE;
    }
}
