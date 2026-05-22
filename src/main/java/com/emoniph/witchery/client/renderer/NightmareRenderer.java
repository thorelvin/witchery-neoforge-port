package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.NightmareModel;
import com.emoniph.witchery.entity.NightmareEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class NightmareRenderer extends MobRenderer<NightmareEntity, NightmareModel.NightmareRenderState, NightmareModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/nightmare.png");

    public NightmareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NightmareModel(ctx.bakeLayer(NightmareModel.LAYER_LOCATION)), 0.6f);
    }

    @Override public NightmareModel.NightmareRenderState createRenderState() { return new NightmareModel.NightmareRenderState(); }

    @Override
    public void extractRenderState(NightmareEntity entity, NightmareModel.NightmareRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        int attackTimer = entity.getAttackTimer();
        state.attackTimer = attackTimer > 0 ? Math.max(0.0f, attackTimer - partialTick) : 0.0f;
        state.entityId = entity.getId();
    }

    @Override public Identifier getTextureLocation(NightmareModel.NightmareRenderState state) { return TEXTURE; }
}
