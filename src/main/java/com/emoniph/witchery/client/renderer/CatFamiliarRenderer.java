package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import net.minecraft.client.model.animal.feline.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;

public class CatFamiliarRenderer extends MobRenderer<CatFamiliarEntity, CatRenderState, CatModel> {

    // All-black vanilla cat skin — fitting for a witch's familiar
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/cat/all_black.png");

    public CatFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatModel(ctx.bakeLayer(ModelLayers.CAT)), 0.4F);
    }

    @Override
    public CatRenderState createRenderState() { return new CatRenderState(); }

    @Override
    public Identifier getTextureLocation(CatRenderState state) { return TEXTURE; }
}
