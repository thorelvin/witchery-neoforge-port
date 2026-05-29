package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WolfmanModel;
import com.emoniph.witchery.entity.WolfmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class WolfmanRenderer extends MobRenderer<WolfmanEntity, WolfmanRenderer.WolfmanRenderState, WolfmanModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/wolfman.png");

    public WolfmanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WolfmanModel(ctx.bakeLayer(WolfmanModel.LAYER_LOCATION)), 0.7F);
    }

    @Override public WolfmanRenderState createRenderState() { return new WolfmanRenderState(); }

    @Override
    public void extractRenderState(WolfmanEntity entity, WolfmanRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTime = entity.getAttackTimer();
    }

    @Override
    public Identifier getTextureLocation(WolfmanRenderState state) { return TEXTURE; }

    public static class WolfmanRenderState extends LivingEntityRenderState {
        public int attackTime;
    }
}
