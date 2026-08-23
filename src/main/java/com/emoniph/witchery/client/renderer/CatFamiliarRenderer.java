package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class CatFamiliarRenderer extends AgeableMobRenderer<CatFamiliarEntity,
        CatFamiliarModel.CatFamiliarRenderState, CatFamiliarModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float SOURCE_TAMED_SCALE = 0.8F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;

    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    public CatFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
                new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION)),
                new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.BABY_LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public CatFamiliarModel.CatFamiliarRenderState createRenderState() {
        return new CatFamiliarModel.CatFamiliarRenderState();
    }

    @Override
    public void extractRenderState(CatFamiliarEntity entity, CatFamiliarModel.CatFamiliarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sitting = entity.isOrderedToSit();
        state.crouching = entity.isCrouching();
        state.sprinting = entity.isSprinting();
        state.tamed = entity.isTame();
    }

    @Override
    protected void scale(CatFamiliarModel.CatFamiliarRenderState state, PoseStack poseStack) {
        applySourceScaleAndAnchor(poseStack, state.tamed);
    }

    static void applySourceScaleAndAnchor(PoseStack poseStack, boolean tamed) {
        if (tamed) {
            poseStack.scale(SOURCE_TAMED_SCALE, SOURCE_TAMED_SCALE, SOURCE_TAMED_SCALE);
        }
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(CatFamiliarModel.CatFamiliarRenderState state) {
        return TEXTURE;
    }
}
