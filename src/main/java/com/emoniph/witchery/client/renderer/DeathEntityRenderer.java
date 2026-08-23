package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.DeathEntityModel;
import com.emoniph.witchery.entity.DeathEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class DeathEntityRenderer extends MobRenderer<DeathEntity, DeathEntityModel.DeathRenderState, DeathEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/death.png");
    public DeathEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DeathEntityModel(ctx.bakeLayer(DeathEntityModel.LAYER_LOCATION)), 0.5f);
    }
    @Override public DeathEntityModel.DeathRenderState createRenderState() { return new DeathEntityModel.DeathRenderState(); }

    @Override
    public void extractRenderState(DeathEntity entity, DeathEntityModel.DeathRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackProgress = entity.getAttackAnim(partialTick);
    }

    @Override public Identifier getTextureLocation(DeathEntityModel.DeathRenderState state) { return TEXTURE; }
}
