package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.emoniph.witchery.entity.familiar.SpectralFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Uses the existing familiar-cat model path for now; the mechanical identity
 * is restored even though the final bespoke spectral-familiar art pass is still open.
 */
public class SpectralFamiliarRenderer extends MobRenderer<SpectralFamiliarEntity, CatFamiliarModel.CatFamiliarRenderState, CatFamiliarModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    public SpectralFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public CatFamiliarModel.CatFamiliarRenderState createRenderState() {
        return new CatFamiliarModel.CatFamiliarRenderState();
    }

    @Override
    public Identifier getTextureLocation(CatFamiliarModel.CatFamiliarRenderState state) {
        return TEXTURE;
    }
}
