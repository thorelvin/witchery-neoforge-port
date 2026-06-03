package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HellhoundModel;
import com.emoniph.witchery.entity.HellhoundEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class HellhoundRenderer extends MobRenderer<HellhoundEntity, LivingEntityRenderState, HellhoundModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/hellhound.png");

    public HellhoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HellhoundModel(ctx.bakeLayer(HellhoundModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
