package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HobgoblinModel;
import com.emoniph.witchery.entity.GoblinMogEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Mog uses the standard goblin model proportions with the mog.png skin. */
public class GoblinMogRenderer extends MobRenderer<GoblinMogEntity, HumanoidRenderState, HobgoblinModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mog.png");

    public GoblinMogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HobgoblinModel<>(ctx.bakeLayer(HobgoblinModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }
    @Override public Identifier getTextureLocation(HumanoidRenderState state) { return TEXTURE; }
}
