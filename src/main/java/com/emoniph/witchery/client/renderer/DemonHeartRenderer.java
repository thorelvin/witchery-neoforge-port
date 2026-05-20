package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.DemonHeartBlock;
import com.emoniph.witchery.blocks.entity.DemonHeartBlockEntity;
import com.emoniph.witchery.client.model.DemonHeartModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DemonHeartRenderer implements BlockEntityRenderer<DemonHeartBlockEntity, DemonHeartRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/demon_heart.png");

    private final DemonHeartModel model;

    public static class State extends BlockEntityRenderState {
        public float facingYaw;
        public long ticks;
    }

    public DemonHeartRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new DemonHeartModel(ctx.bakeLayer(DemonHeartModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DemonHeartBlockEntity be, State state, float partialTick, Vec3 offset,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        BlockState blockState = be.getBlockState();
        state.facingYaw = blockState.hasProperty(DemonHeartBlock.FACING)
                ? blockState.getValue(DemonHeartBlock.FACING).toYRot()
                : 0.0F;
        state.ticks = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.7F, 0.7F, 0.7F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facingYaw));
        poseStack.translate(0.1F, -0.9F, -0.15F);

        for (var part : model.staticParts()) {
            collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }

        double pulse = 0.165D * (7.0D + 0.25D * Math.sin(0.25132741228718347D * state.ticks));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.scale((float) pulse, (float) pulse, (float) pulse);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        for (var part : model.pulseParts()) {
            collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        poseStack.popPose();

        poseStack.popPose();
    }
}
