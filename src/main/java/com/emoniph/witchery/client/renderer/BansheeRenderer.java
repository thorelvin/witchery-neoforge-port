package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.GhostModel;
import com.emoniph.witchery.entity.BansheeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class BansheeRenderer extends MobRenderer<BansheeEntity, GhostModel.GhostRenderState, GhostModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/banshee.png");
    @Override
    public GhostModel.GhostRenderState createRenderState() { return new GhostModel.GhostRenderState(); }

    public BansheeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GhostModel(ctx.bakeLayer(GhostModel.LAYER_BANSHEE), false), 0.5F);
    }

    @Override
    public void extractRenderState(BansheeEntity entity, GhostModel.GhostRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.screaming = entity.isScreaming();
    }

    @Override public Identifier getTextureLocation(GhostModel.GhostRenderState state) { return TEXTURE; }
}
