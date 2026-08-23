package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.BloodCrucibleBlockEntity;
import com.emoniph.witchery.client.model.BloodCrucibleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BloodCrucibleRenderer
        implements BlockEntityRenderer<BloodCrucibleBlockEntity, BloodCrucibleRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/blood_crucible.png");
    private static final Identifier LAVA_TEXTURE =
            Identifier.withDefaultNamespace("textures/block/lava_still.png");
    private static final int BLOOD_COLOR =
            ARGB.colorFromFloat(0.9F, 255.0F / 256.0F, 0.0F, 0.0F);

    private final BloodCrucibleModel model;

    public static class State extends BlockEntityRenderState {
        public float bloodPercent;
    }

    public BloodCrucibleRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BloodCrucibleModel(
                context.bakeLayer(BloodCrucibleModel.LAYER_LOCATION),
                context.bakeLayer(BloodCrucibleModel.LIQUID_LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BloodCrucibleBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, offset, crumbling);
        state.bloodPercent = blockEntity.getPercentFilled();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        applySourcePose(poseStack);

        for (ModelPart part : model.bodyParts()) {
            collector.submitModelPart(part, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }

        if (state.bloodPercent > 0.0F) {
            model.setBloodPercent(state.bloodPercent);
            collector.submitModelPart(model.blood(), poseStack, RenderTypes.entityTranslucent(LAVA_TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null, BLOOD_COLOR, null);
        }

        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }
}
