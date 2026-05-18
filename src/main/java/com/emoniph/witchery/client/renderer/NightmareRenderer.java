package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.NightmareModel;
import com.emoniph.witchery.entity.NightmareEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class NightmareRenderer extends MobRenderer<NightmareEntity, LivingEntityRenderState, NightmareModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/nightmare.png");
    public NightmareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NightmareModel(ctx.bakeLayer(NightmareModel.LAYER_LOCATION)), 0.6f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
}
