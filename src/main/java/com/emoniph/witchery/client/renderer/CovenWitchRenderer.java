package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.CovenWitchEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.Identifier;

public class CovenWitchRenderer extends MobRenderer<CovenWitchEntity, CovenWitchRenderer.CovenWitchRenderState, WitchModel> {

    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch3.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch4.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch5.png"),
    };

    public CovenWitchRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchModel(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F);
    }

    @Override public CovenWitchRenderState createRenderState() { return new CovenWitchRenderState(); }

    @Override
    public void extractRenderState(CovenWitchEntity entity, CovenWitchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = entity.getSkin();
    }

    @Override
    public Identifier getTextureLocation(CovenWitchRenderState state) {
        return SKINS[Math.floorMod(state.skin, SKINS.length)];
    }

    public static class CovenWitchRenderState extends WitchRenderState {
        public int skin;
    }
}
