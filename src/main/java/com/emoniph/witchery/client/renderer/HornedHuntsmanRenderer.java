package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HornedHuntsmanModel;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class HornedHuntsmanRenderer extends MobRenderer<HornedHuntsmanEntity, LivingEntityRenderState, HornedHuntsmanModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/horned_huntsman.png");

    public HornedHuntsmanRenderer(EntityRendererProvider.Context context) {
        super(context, new HornedHuntsmanModel(context.bakeLayer(HornedHuntsmanModel.LAYER_LOCATION)), 0.5F);
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
