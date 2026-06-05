package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.CrazyCatLadyEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.Identifier;

public class CrazyCatLadyRenderer extends MobRenderer<CrazyCatLadyEntity, CrazyCatLadyRenderer.CrazyCatLadyRenderState, WitchModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/crazy_cat_lady.png");

    public CrazyCatLadyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchModel(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F);
        addLayer(new CrazyCatLadyHeldCatLayer(this, ctx));
    }

    @Override
    public CrazyCatLadyRenderState createRenderState() {
        return new CrazyCatLadyRenderState();
    }

    @Override
    public void extractRenderState(CrazyCatLadyEntity entity, CrazyCatLadyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.catReady = entity.hasCatReady();
    }

    @Override
    public Identifier getTextureLocation(CrazyCatLadyRenderState state) {
        return TEXTURE;
    }

    public static class CrazyCatLadyRenderState extends WitchRenderState {
        public boolean catReady;
    }
}
