package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.emoniph.witchery.entity.familiar.SpectralFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Uses the existing familiar-cat model path for now; the mechanical identity
 * is restored even though the final bespoke spectral-familiar art pass is still open.
 */
public class SpectralFamiliarRenderer extends MobRenderer<SpectralFamiliarEntity, LivingEntityRenderState, CatFamiliarModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    public SpectralFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION)), 0.3F);
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
