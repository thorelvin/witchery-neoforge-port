package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.ToadModel;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class ToadFamiliarRenderer extends MobRenderer<ToadFamiliarEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, ToadModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/toad.png");

    public ToadFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ToadModel(ctx.bakeLayer(ToadModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
