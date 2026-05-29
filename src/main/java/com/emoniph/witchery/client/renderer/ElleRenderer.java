package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.ElleEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.Identifier;

public class ElleRenderer extends MobRenderer<ElleEntity, WitchRenderState, WitchModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/elle.png");

    public ElleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchModel(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F);
    }

    @Override
    public WitchRenderState createRenderState() {
        return new WitchRenderState();
    }

    @Override
    public Identifier getTextureLocation(WitchRenderState state) {
        return TEXTURE;
    }
}
