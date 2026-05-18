package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.EntModel;
import com.emoniph.witchery.entity.EntEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class EntRenderer extends MobRenderer<EntEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, EntModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/ent.png");
    public EntRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EntModel(ctx.bakeLayer(EntModel.LAYER_LOCATION)), 0.8F);
    }
    @Override public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
