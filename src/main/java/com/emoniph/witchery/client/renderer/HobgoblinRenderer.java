package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HobgoblinModel;
import com.emoniph.witchery.entity.HobgoblinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class HobgoblinRenderer extends HumanoidMobRenderer<HobgoblinEntity, HobgoblinRenderer.HobgoblinRenderState, HobgoblinModel<HobgoblinRenderer.HobgoblinRenderState>> {

    public static class HobgoblinRenderState extends HumanoidRenderState {
        public int profession = 0;
    }

    private static final Identifier[] TEXTURES = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin3.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin4.png"),
    };

    public HobgoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HobgoblinModel<>(ctx.bakeLayer(HobgoblinModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public HobgoblinRenderState createRenderState() { return new HobgoblinRenderState(); }

    @Override
    public void extractRenderState(HobgoblinEntity entity, HobgoblinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.profession = entity.getProfessionVariant();
    }

    @Override
    public Identifier getTextureLocation(HobgoblinRenderState state) {
        return TEXTURES[Math.floorMod(state.profession, TEXTURES.length)];
    }
}
