package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.SpectralFamiliarModel;
import com.emoniph.witchery.entity.familiar.SpectralFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class SpectralFamiliarRenderer extends MobRenderer<SpectralFamiliarEntity,
        SpectralFamiliarModel.State, SpectralFamiliarModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float SOURCE_TAMED_SCALE = 0.8F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final int IDLE_TINT = ARGB.colorFromFloat(0.51F, 0.5F, 0.5F, 1.0F);
    static final int SEARCHING_TINT = ARGB.colorFromFloat(0.51F, 0.7F, 0.3F, 1.0F);

    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/spectral_familiar.png");

    public SpectralFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpectralFamiliarModel(ctx.bakeLayer(SpectralFamiliarModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public SpectralFamiliarModel.State createRenderState() {
        return new SpectralFamiliarModel.State();
    }

    @Override
    public void extractRenderState(SpectralFamiliarEntity entity, SpectralFamiliarModel.State state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sitting = entity.isOrderedToSit();
        state.tamed = entity.isTame();
        state.searching = entity.isSearchingForOre();
    }

    @Override
    protected void scale(SpectralFamiliarModel.State state, PoseStack poseStack) {
        applySourceScaleAndAnchor(poseStack, state.tamed);
    }

    @Override
    protected int getModelTint(SpectralFamiliarModel.State state) {
        return state.searching ? SEARCHING_TINT : IDLE_TINT;
    }

    @Override
    protected @Nullable RenderType getRenderType(SpectralFamiliarModel.State state,
                                                  boolean bodyVisible,
                                                  boolean translucent,
                                                  boolean glowing) {
        if (bodyVisible) {
            return RenderTypes.entityTranslucent(TEXTURE);
        }
        return super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    public Identifier getTextureLocation(SpectralFamiliarModel.State state) {
        return TEXTURE;
    }

    static void applySourceScaleAndAnchor(PoseStack poseStack, boolean tamed) {
        if (tamed) {
            poseStack.scale(SOURCE_TAMED_SCALE, SOURCE_TAMED_SCALE, SOURCE_TAMED_SCALE);
        }
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }
}
