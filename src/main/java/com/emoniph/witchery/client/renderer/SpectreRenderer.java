package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GhostModel;
import com.emoniph.witchery.entity.SpectreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class SpectreRenderer extends MobRenderer<SpectreEntity, GhostModel.GhostRenderState, GhostModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final int SOURCE_NORMAL_TINT = 0x99FFFFFF;
    static final int SOURCE_OBSCURED_TINT = 0x26FFFFFF;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/spectre.png");
    @Override
    public GhostModel.GhostRenderState createRenderState() { return new GhostModel.GhostRenderState(); }

    public SpectreRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GhostModel(ctx.bakeLayer(GhostModel.LAYER_SPECTRE), true), SOURCE_SHADOW_RADIUS);
    }

    @Override
    public void extractRenderState(SpectreEntity entity, GhostModel.GhostRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.obscured = entity.isObscured();
    }

    @Override
    protected RenderType getRenderType(GhostModel.GhostRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE)
                : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(GhostModel.GhostRenderState state) {
        return state.obscured ? SOURCE_OBSCURED_TINT : SOURCE_NORMAL_TINT;
    }

    @Override
    protected void scale(GhostModel.GhostRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override public Identifier getTextureLocation(GhostModel.GhostRenderState state) { return TEXTURE; }
}
