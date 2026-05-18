package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.OwlModel;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class OwlFamiliarRenderer extends MobRenderer<OwlFamiliarEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, OwlModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/owl.png");

    public OwlFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new OwlModel(ctx.bakeLayer(OwlModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
