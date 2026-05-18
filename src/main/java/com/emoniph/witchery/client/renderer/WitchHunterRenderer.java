package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WitchHunterModel;
import com.emoniph.witchery.entity.WitchHunterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class WitchHunterRenderer extends MobRenderer<WitchHunterEntity, LivingEntityRenderState, WitchHunterModel> {

    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter3.png"),
    };

    public WitchHunterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchHunterModel(ctx.bakeLayer(WitchHunterModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) {
        return SKINS[0];
    }
}
