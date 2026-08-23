package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.ToadModel;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class ToadFamiliarRenderer extends AgeableMobRenderer<ToadFamiliarEntity,
        ToadModel.ToadRenderState, ToadModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.3F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/toad.png");
    private static final float[][] FLEECE_COLOR_TABLE = new float[][]{
            {1.0F, 1.0F, 1.0F}, {0.85F, 0.5F, 0.2F}, {0.7F, 0.3F, 0.85F},
            {0.4F, 0.6F, 0.85F}, {0.9F, 0.9F, 0.2F}, {0.5F, 0.8F, 0.1F},
            {0.95F, 0.5F, 0.65F}, {0.3F, 0.3F, 0.3F}, {0.6F, 0.6F, 0.6F},
            {0.3F, 0.5F, 0.6F}, {0.5F, 0.25F, 0.7F}, {0.2F, 0.3F, 0.7F},
            {0.4F, 0.3F, 0.2F}, {0.4F, 0.5F, 0.2F}, {0.6F, 0.2F, 0.2F},
            {0.1F, 0.1F, 0.1F}
    };

    public ToadFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ToadModel(context.bakeLayer(ToadModel.LAYER_LOCATION)),
                new ToadModel(context.bakeLayer(ToadModel.BABY_LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public ToadModel.ToadRenderState createRenderState() {
        return new ToadModel.ToadRenderState();
    }

    @Override
    public void extractRenderState(ToadFamiliarEntity entity, ToadModel.ToadRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sitting = entity.isOrderedToSit();
        state.skinColor = entity.getSkinColor();
        state.tintColor = skinTint(state.skinColor);
    }

    @Override
    protected void scale(ToadModel.ToadRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override
    protected int getModelTint(ToadModel.ToadRenderState state) {
        return state.tintColor;
    }

    @Override
    public Identifier getTextureLocation(ToadModel.ToadRenderState state) {
        return TEXTURE;
    }

    static int skinTint(int skinColor) {
        int id = skinColor & 15;
        if (id == 0) {
            return -1;
        }
        float[] dye = FLEECE_COLOR_TABLE[id];
        return ARGB.color(255,
                toChannel(dye[0] + 0.065205686F),
                toChannel(dye[1] + 0.04921184F),
                toChannel(dye[2] + 0.038139176F));
    }

    private static int toChannel(float value) {
        return Mth.clamp((int) (value * 255.0F), 0, 255);
    }
}
