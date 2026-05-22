package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CatFamiliarRenderer extends MobRenderer<CatFamiliarEntity, CatFamiliarModel.CatFamiliarRenderState, CatFamiliarModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    public CatFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public CatFamiliarModel.CatFamiliarRenderState createRenderState() {
        return new CatFamiliarModel.CatFamiliarRenderState();
    }

    @Override
    public void extractRenderState(CatFamiliarEntity entity, CatFamiliarModel.CatFamiliarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sitting = entity.isOrderedToSit();
    }

    @Override
    public Identifier getTextureLocation(CatFamiliarModel.CatFamiliarRenderState state) {
        return TEXTURE;
    }
}
