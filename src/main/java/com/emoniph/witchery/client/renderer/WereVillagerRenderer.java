package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WitchHunterModel;
import com.emoniph.witchery.entity.WereVillagerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Renders WereVillagerEntity using the standard biped model with a dedicated texture. */
public class WereVillagerRenderer extends MobRenderer<WereVillagerEntity, LivingEntityRenderState, WitchHunterModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/were_villager.png");

    public WereVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchHunterModel(ctx.bakeLayer(WitchHunterModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
