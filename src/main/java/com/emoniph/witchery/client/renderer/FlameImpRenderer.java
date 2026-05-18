package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.FlameImpModel;
import com.emoniph.witchery.entity.FlameImpEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FlameImpRenderer extends MobRenderer<FlameImpEntity, LivingEntityRenderState, FlameImpModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/imp.png");
    public FlameImpRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlameImpModel(ctx.bakeLayer(FlameImpModel.LAYER_LOCATION)), 0.4f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
