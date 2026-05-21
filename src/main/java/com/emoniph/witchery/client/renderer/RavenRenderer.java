package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.RavenModel;
import com.emoniph.witchery.entity.RavenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class RavenRenderer extends MobRenderer<RavenEntity, LivingEntityRenderState, RavenModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/raven.png");

    public RavenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RavenModel(ctx.bakeLayer(RavenModel.LAYER_LOCATION)), 0.35F);
        this.addLayer(new RavenEyesLayer(this));
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
