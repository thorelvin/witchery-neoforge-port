package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.RavenModel;
import com.emoniph.witchery.entity.RavenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class RavenRenderer extends MobRenderer<RavenEntity, RavenModel.RavenRenderState, RavenModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/raven.png");

    public RavenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RavenModel(ctx.bakeLayer(RavenModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public RavenModel.RavenRenderState createRenderState() {
        return new RavenModel.RavenRenderState();
    }

    @Override
    public void extractRenderState(RavenEntity entity, RavenModel.RavenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.flying = !entity.onGround();
    }

    @Override
    public Identifier getTextureLocation(RavenModel.RavenRenderState state) {
        return TEXTURE;
    }
}
