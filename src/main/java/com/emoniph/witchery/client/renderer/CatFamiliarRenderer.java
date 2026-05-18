package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CatFamiliarRenderer extends MobRenderer<CatFamiliarEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, CatFamiliarModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    public CatFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
