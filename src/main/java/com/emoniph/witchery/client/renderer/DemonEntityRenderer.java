package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.DemonEntityModel;
import com.emoniph.witchery.entity.DemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class DemonEntityRenderer extends MobRenderer<DemonEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, DemonEntityModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/demon.png");
    public DemonEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DemonEntityModel(ctx.bakeLayer(DemonEntityModel.LAYER_LOCATION)), 0.7F);
    }
    @Override public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
