package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BabaYagaModel;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BabaYagaRenderer extends MobRenderer<BabaYagaEntity,
        BabaYagaModel.BabaYagaRenderState, BabaYagaModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float SOURCE_SCALE = 0.9375F;
    static final float SOURCE_MODEL_TRANSLATION_Y = -0.2F;
    static final float LEGACY_LIVING_BASE_Y = -1.5078125F;
    static final float MODERN_LIVING_BASE_Y = -1.501F;
    static final float SOURCE_VERTICAL_COMPENSATION =
            SOURCE_MODEL_TRANSLATION_Y + LEGACY_LIVING_BASE_Y - MODERN_LIVING_BASE_Y;

    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/babayaga.png");

    public BabaYagaRenderer(EntityRendererProvider.Context context) {
        super(context, new BabaYagaModel(context.bakeLayer(BabaYagaModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
        this.addLayer(new BabaYagaHeldItemLayer(this));
    }

    @Override
    public BabaYagaModel.BabaYagaRenderState createRenderState() {
        return new BabaYagaModel.BabaYagaRenderState();
    }

    @Override
    public void extractRenderState(BabaYagaEntity entity, BabaYagaModel.BabaYagaRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.entityId = entity.getId();
        state.entityTick = entity.tickCount;
        state.isHoldingItem = !entity.getMainHandItem().isEmpty();
    }

    @Override
    protected void scale(BabaYagaModel.BabaYagaRenderState state, PoseStack poseStack) {
        applySourceScaleAndVerticalAnchor(poseStack);
    }

    static void applySourceScaleAndVerticalAnchor(PoseStack poseStack) {
        poseStack.scale(SOURCE_SCALE, SOURCE_SCALE, SOURCE_SCALE);
        poseStack.translate(0.0F, SOURCE_VERTICAL_COMPENSATION, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(BabaYagaModel.BabaYagaRenderState state) {
        return TEXTURE;
    }
}
