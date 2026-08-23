package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.PoltergeistModel;
import com.emoniph.witchery.entity.PoltergeistEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class PoltergeistRenderer extends MobRenderer<PoltergeistEntity, PoltergeistModel.PoltergeistRenderState, PoltergeistModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final int SOURCE_TINT = 0x66FFFFFF;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/poltergeist.png");

    public PoltergeistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PoltergeistModel(ctx.bakeLayer(PoltergeistModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override public PoltergeistModel.PoltergeistRenderState createRenderState() {
        return new PoltergeistModel.PoltergeistRenderState();
    }

    @Override
    public void extractRenderState(PoltergeistEntity entity, PoltergeistModel.PoltergeistRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTimer = entity.getAttackTimer();
    }

    @Override
    protected RenderType getRenderType(PoltergeistModel.PoltergeistRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE)
                : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(PoltergeistModel.PoltergeistRenderState state) {
        return SOURCE_TINT;
    }

    @Override
    protected void scale(PoltergeistModel.PoltergeistRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override public Identifier getTextureLocation(PoltergeistModel.PoltergeistRenderState state) { return TEXTURE; }
}
