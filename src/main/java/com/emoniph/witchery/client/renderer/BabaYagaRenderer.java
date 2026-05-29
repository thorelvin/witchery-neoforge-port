package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BabaYagaModel;
import com.emoniph.witchery.entity.BabaYagaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BabaYagaRenderer extends MobRenderer<BabaYagaEntity, LivingEntityRenderState, BabaYagaModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/babayaga.png");

    public BabaYagaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BabaYagaModel(ctx.bakeLayer(BabaYagaModel.LAYER_LOCATION)), 0.7F);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
