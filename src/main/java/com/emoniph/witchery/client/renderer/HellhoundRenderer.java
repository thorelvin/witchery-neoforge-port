package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HellhoundModel;
import com.emoniph.witchery.entity.HellhoundEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class HellhoundRenderer extends MobRenderer<HellhoundEntity, HellhoundModel.HellhoundRenderState, HellhoundModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/hellhound.png");

    public HellhoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HellhoundModel(ctx.bakeLayer(HellhoundModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public HellhoundModel.HellhoundRenderState createRenderState() {
        return new HellhoundModel.HellhoundRenderState();
    }

    @Override
    public void extractRenderState(HellhoundEntity entity, HellhoundModel.HellhoundRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.converting = entity.isConverting();
        state.headRoll = entity.getBodyRollAngle(partialTick, 0.0F);
        state.maneRoll = entity.getBodyRollAngle(partialTick, -0.08F);
        state.bodyRoll = entity.getBodyRollAngle(partialTick, -0.16F);
        state.tailRoll = entity.getBodyRollAngle(partialTick, -0.2F);
    }

    @Override
    public Identifier getTextureLocation(HellhoundModel.HellhoundRenderState state) {
        return TEXTURE;
    }
}
