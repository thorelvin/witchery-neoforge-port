package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HobgoblinModel;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Gulg uses the standard goblin model proportions with the gulg.png skin. */
public class GoblinGulgRenderer extends MobRenderer<GoblinGulgEntity, HumanoidRenderState, HobgoblinModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/gulg.png");

    public GoblinGulgRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HobgoblinModel<>(ctx.bakeLayer(HobgoblinModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }
    @Override public Identifier getTextureLocation(HumanoidRenderState state) { return TEXTURE; }
}
