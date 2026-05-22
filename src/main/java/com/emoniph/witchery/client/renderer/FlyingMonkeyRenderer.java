package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.FlyingMonkeyModel;
import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class FlyingMonkeyRenderer extends MobRenderer<FlyingMonkeyEntity, FlyingMonkeyModel.FlyingMonkeyRenderState, FlyingMonkeyModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/flying_monkey.png");
    private static final float[][] FLEECE_COLOR_TABLE = new float[][]{
            {1.0F, 1.0F, 1.0F},
            {0.85F, 0.5F, 0.2F},
            {0.7F, 0.3F, 0.85F},
            {0.4F, 0.6F, 0.85F},
            {0.9F, 0.9F, 0.2F},
            {0.5F, 0.8F, 0.1F},
            {0.95F, 0.5F, 0.65F},
            {0.3F, 0.3F, 0.3F},
            {0.6F, 0.6F, 0.6F},
            {0.3F, 0.5F, 0.6F},
            {0.5F, 0.25F, 0.7F},
            {0.2F, 0.3F, 0.7F},
            {0.4F, 0.3F, 0.2F},
            {0.4F, 0.5F, 0.2F},
            {0.6F, 0.2F, 0.2F},
            {0.1F, 0.1F, 0.1F}
    };

    public FlyingMonkeyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlyingMonkeyModel(ctx.bakeLayer(FlyingMonkeyModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public FlyingMonkeyModel.FlyingMonkeyRenderState createRenderState() {
        return new FlyingMonkeyModel.FlyingMonkeyRenderState();
    }

    @Override
    public void extractRenderState(FlyingMonkeyEntity entity, FlyingMonkeyModel.FlyingMonkeyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sitting = entity.isOrderedToSit();
        state.landed = entity.onGround() && entity.getDeltaMovement().lengthSqr() < 1.0E-5D;
        state.tintColor = featherTint(entity.getDyeColor());
    }

    @Override
    public Identifier getTextureLocation(FlyingMonkeyModel.FlyingMonkeyRenderState state) { return TEXTURE; }

    @Override
    protected int getModelTint(FlyingMonkeyModel.FlyingMonkeyRenderState state) {
        return state.tintColor;
    }

    private static int featherTint(DyeColor color) {
        int id = color.getId() & 15;
        if (id == 0) {
            return -1;
        }

        float[] dye = FLEECE_COLOR_TABLE[id];
        int red = toChannel(dye[0] * 0.15686274F + 0.41568628F);
        int green = toChannel(dye[1] * 0.15686274F + 0.3137255F);
        int blue = toChannel(dye[2] * 0.15686274F + 0.24313726F);
        return ARGB.color(255, red, green, blue);
    }

    private static int toChannel(float value) {
        return Mth.clamp((int)(value * 255.0F), 0, 255);
    }
}
