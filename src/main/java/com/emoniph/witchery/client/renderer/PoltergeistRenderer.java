package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.PoltergeistModel;
import com.emoniph.witchery.entity.PoltergeistEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class PoltergeistRenderer extends MobRenderer<PoltergeistEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, PoltergeistModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/poltergeist.png");

    public PoltergeistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PoltergeistModel(ctx.bakeLayer(PoltergeistModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
