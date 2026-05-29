package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LilithModel;
import com.emoniph.witchery.entity.LilithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class LilithRenderer extends MobRenderer<LilithEntity, LilithModel.LilithRenderState, LilithModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/lilith.png");

    public LilithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LilithModel(ctx.bakeLayer(LilithModel.LAYER_LOCATION)), 0.8F);
    }

    @Override
    public LilithModel.LilithRenderState createRenderState() {
        return new LilithModel.LilithRenderState();
    }

    @Override
    public void extractRenderState(LilithEntity entity, LilithModel.LilithRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTimer = entity.getAttackTimer();
        state.isFriendly  = entity.isFriendly();
    }

    @Override
    public Identifier getTextureLocation(LilithModel.LilithRenderState state) {
        return TEXTURE;
    }
}
