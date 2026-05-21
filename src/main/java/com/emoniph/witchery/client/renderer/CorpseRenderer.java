package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WitchHunterModel;
import com.emoniph.witchery.entity.CorpseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Simple visible sleeping-body renderer for Spirit World corpses.
 * This uses an existing humanoid body model so the corpse is no longer
 * invisible without taking on a larger owner-skin sync project.
 */
public class CorpseRenderer extends MobRenderer<CorpseEntity, LivingEntityRenderState, WitchHunterModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter1.png");

    public CorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchHunterModel(ctx.bakeLayer(WitchHunterModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
