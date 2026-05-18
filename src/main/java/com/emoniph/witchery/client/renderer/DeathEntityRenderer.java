package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.DeathEntityModel;
import com.emoniph.witchery.entity.DeathEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class DeathEntityRenderer extends MobRenderer<DeathEntity, LivingEntityRenderState, DeathEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/death.png");
    public DeathEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DeathEntityModel(ctx.bakeLayer(DeathEntityModel.LAYER_LOCATION)), 0.5f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
