package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.MindrakeModel;
import com.emoniph.witchery.entity.MindrakeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class MindrakeRenderer extends MobRenderer<MindrakeEntity, LivingEntityRenderState, MindrakeModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mindrake.png");

    public MindrakeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MindrakeModel(ctx.bakeLayer(MindrakeModel.LAYER_LOCATION)), 0.3F);
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
