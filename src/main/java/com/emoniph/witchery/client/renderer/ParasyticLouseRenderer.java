package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.ParasyticLouseModel;
import com.emoniph.witchery.entity.ParasyticLouseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class ParasyticLouseRenderer extends MobRenderer<ParasyticLouseEntity, LivingEntityRenderState, ParasyticLouseModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/louse.png");

    public ParasyticLouseRenderer(EntityRendererProvider.Context context) {
        super(context, new ParasyticLouseModel(context.bakeLayer(ParasyticLouseModel.LAYER_LOCATION)), 0.2F);
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
