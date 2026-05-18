package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.CovenWitchEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.Identifier;

public class CovenWitchRenderer extends MobRenderer<CovenWitchEntity, WitchRenderState, WitchModel> {

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

    @Override public WitchRenderState createRenderState() { return new WitchRenderState(); }

    @Override
    public void extractRenderState(CovenWitchEntity entity, WitchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
    }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.WitchRenderState state) {
        int skin = Math.abs(state.entityId) % SKINS.length;
        return SKINS[skin];
    }
}
