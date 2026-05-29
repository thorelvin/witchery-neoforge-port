package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GoblinGulgModel;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class GoblinGulgRenderer extends MobRenderer<GoblinGulgEntity, LivingEntityRenderState, GoblinGulgModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/gulg.png");

    public GoblinGulgRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinGulgModel(ctx.bakeLayer(GoblinGulgModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
