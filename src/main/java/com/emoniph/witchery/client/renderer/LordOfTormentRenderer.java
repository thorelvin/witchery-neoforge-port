package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LordOfTormentModel;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class LordOfTormentRenderer extends MobRenderer<LordOfTormentEntity,
        LordOfTormentModel.LordOfTormentRenderState, LordOfTormentModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/lord_of_torment.png");
    public LordOfTormentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LordOfTormentModel(ctx.bakeLayer(LordOfTormentModel.LAYER_LOCATION)), 0.5f);
    }
    @Override
    public LordOfTormentModel.LordOfTormentRenderState createRenderState() {
        return new LordOfTormentModel.LordOfTormentRenderState();
    }

    @Override
    public void extractRenderState(LordOfTormentEntity entity,
                                   LordOfTormentModel.LordOfTormentRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTimer = Math.max(0.0F, entity.getAttackTimer() - partialTick);
        state.moving = entity.getDeltaMovement().horizontalDistanceSqr() > 0.001D;
    }

    @Override
    public Identifier getTextureLocation(LordOfTormentModel.LordOfTormentRenderState state) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(LordOfTormentEntity entity, BlockPos pos) {
        return 15;
    }
}
