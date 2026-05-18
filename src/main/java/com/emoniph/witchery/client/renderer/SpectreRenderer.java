package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GhostModel;
import com.emoniph.witchery.entity.SpectreEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class SpectreRenderer extends MobRenderer<SpectreEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, GhostModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/spectre.png");
    @Override
    public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }

    public SpectreRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GhostModel(ctx.bakeLayer(GhostModel.LAYER_SPECTRE), true), 0.5F);
    }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
