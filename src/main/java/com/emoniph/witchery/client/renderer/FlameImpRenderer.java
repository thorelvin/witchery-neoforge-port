package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.FlameImpModel;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FlameImpRenderer extends MobRenderer<FlameImpEntity, FlameImpModel.FlameImpRenderState, FlameImpModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/imp.png");
    public FlameImpRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlameImpModel(ctx.bakeLayer(FlameImpModel.LAYER_LOCATION)), 0.5f);
    }

    @Override public FlameImpModel.FlameImpRenderState createRenderState() { return new FlameImpModel.FlameImpRenderState(); }

    @Override
    public void extractRenderState(FlameImpEntity entity, FlameImpModel.FlameImpRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackProgress = entity.getAttackAnim(partialTick);
        state.powered = entity.isPowered();
    }

    @Override public Identifier getTextureLocation(FlameImpModel.FlameImpRenderState state) { return TEXTURE; }

    @Override
    protected void scale(FlameImpModel.FlameImpRenderState state, PoseStack ps) {
        if (state.powered) {
            ps.scale(1.5F, 1.0F, 1.5F);
        }
    }
}
