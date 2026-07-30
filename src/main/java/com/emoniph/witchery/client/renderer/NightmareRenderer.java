package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.NightmareModel;
import com.emoniph.witchery.entity.NightmareEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class NightmareRenderer extends MobRenderer<NightmareEntity, NightmareModel.NightmareRenderState, NightmareModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/nightmare.png");

    public NightmareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NightmareModel(ctx.bakeLayer(NightmareModel.LAYER_LOCATION)),
                NightmareEntity.SOURCE_SHADOW_RADIUS);
    }

    @Override public NightmareModel.NightmareRenderState createRenderState() { return new NightmareModel.NightmareRenderState(); }

    @Override
    public void extractRenderState(NightmareEntity entity, NightmareModel.NightmareRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        int attackTimer = entity.getAttackTimer();
        state.attackTimer = attackTimer > 0 ? Math.max(0.0f, attackTimer - partialTick) : 0.0f;
        state.entityId = entity.getId();
        state.defended = entity.isDefended();
    }

    @Override public Identifier getTextureLocation(NightmareModel.NightmareRenderState state) { return TEXTURE; }

    @Override
    protected RenderType getRenderType(NightmareModel.NightmareRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE)
                : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(NightmareModel.NightmareRenderState state) {
        return state.defended ? NightmareEntity.SOURCE_DEFENDED_ALPHA : NightmareEntity.SOURCE_NORMAL_ALPHA;
    }
}
