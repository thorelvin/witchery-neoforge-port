package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.TreefydModel;
import com.emoniph.witchery.entity.TreefydEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class TreefydRenderer extends MobRenderer<TreefydEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, TreefydModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/treefyd.png");
    public TreefydRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TreefydModel(ctx.bakeLayer(TreefydModel.LAYER_LOCATION)), 0.5F);
    }
    @Override public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
