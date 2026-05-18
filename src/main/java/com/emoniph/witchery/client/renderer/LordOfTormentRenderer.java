package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LordOfTormentModel;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class LordOfTormentRenderer extends MobRenderer<LordOfTormentEntity, LivingEntityRenderState, LordOfTormentModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/lord_of_torment.png");
    public LordOfTormentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LordOfTormentModel(ctx.bakeLayer(LordOfTormentModel.LAYER_LOCATION)), 0.7f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
