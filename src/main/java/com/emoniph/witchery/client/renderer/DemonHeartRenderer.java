package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.DemonHeartBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DemonHeartRenderer implements BlockEntityRenderer<DemonHeartBlockEntity, DemonHeartRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public long ticks;
    }

    public DemonHeartRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DemonHeartBlockEntity be, State state, float partialTick, Vec3 offset,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.ticks = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float pulse = 1.0F + 0.04F * (0.5F + 0.5F * Mth.sin(state.ticks * 0.2513274F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        collector.submitBlock(poseStack, state.blockState, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
