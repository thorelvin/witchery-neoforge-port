package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.MandrakeEntityModel;
import com.emoniph.witchery.entity.MandrakeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class MandrakeEntityRenderer extends MobRenderer<MandrakeEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, MandrakeEntityModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mandrake.png");
    public MandrakeEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MandrakeEntityModel(ctx.bakeLayer(MandrakeEntityModel.LAYER_LOCATION)), 0.3F);
    }
    @Override public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
