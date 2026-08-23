package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GhostModel;
import com.emoniph.witchery.entity.BansheeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BansheeRenderer extends MobRenderer<BansheeEntity, GhostModel.GhostRenderState, GhostModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final int SOURCE_TINT = 0xB3FFFFFF;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/banshee.png");
    @Override
    public GhostModel.GhostRenderState createRenderState() { return new GhostModel.GhostRenderState(); }

    public BansheeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GhostModel(ctx.bakeLayer(GhostModel.LAYER_BANSHEE), false), SOURCE_SHADOW_RADIUS);
    }

    @Override
    public void extractRenderState(BansheeEntity entity, GhostModel.GhostRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.screaming = entity.isScreaming();
    }

    @Override
    protected RenderType getRenderType(GhostModel.GhostRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE)
                : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(GhostModel.GhostRenderState state) {
        return SOURCE_TINT;
    }

    @Override
    protected void scale(GhostModel.GhostRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override public Identifier getTextureLocation(GhostModel.GhostRenderState state) { return TEXTURE; }
}
