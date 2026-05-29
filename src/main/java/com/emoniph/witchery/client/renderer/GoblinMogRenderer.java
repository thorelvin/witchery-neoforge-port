package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GoblinMogModel;
import com.emoniph.witchery.entity.GoblinMogEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class GoblinMogRenderer extends MobRenderer<GoblinMogEntity, LivingEntityRenderState, GoblinMogModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mog.png");

    public GoblinMogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinMogModel(ctx.bakeLayer(GoblinMogModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
