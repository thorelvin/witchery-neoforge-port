package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BroomModel;
import com.emoniph.witchery.entity.BroomEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class BroomRenderer extends MobRenderer<BroomEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, BroomModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/broom.png");

    public BroomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BroomModel(ctx.bakeLayer(BroomModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
