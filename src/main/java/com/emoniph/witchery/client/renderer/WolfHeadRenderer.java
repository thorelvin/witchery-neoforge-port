package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.WolfHeadBlock;
import com.emoniph.witchery.blocks.entity.WolfHeadBlockEntity;
import com.emoniph.witchery.client.model.WolfHeadModel;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;

/**
 * BESR for the mounted wolf/hellhound head. Mirrors {@code AlluringSkullRenderer}'s transform
 * (per-facing translate, scale -1/-1/1, yaw rotation) and {@code RenderWolfHead}'s texture split.
 */
public class WolfHeadRenderer implements BlockEntityRenderer<WolfHeadBlockEntity, WolfHeadRenderer.State> {

    private static final Identifier WOLF_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/wolf/wolf.png");
    private static final Identifier HELLHOUND_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/hellhound.png");

    private final WolfHeadModel model;

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.UP;
        public boolean hellhound;
        public float yaw;
    }

    public WolfHeadRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new WolfHeadModel(context.bakeLayer(WolfHeadModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WolfHeadBlockEntity blockEntity, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(WolfHeadBlock.FACING);
        state.hellhound = blockState.getBlock() instanceof WolfHeadBlock head && head.isHellhound();
        state.yaw = state.facing == Direction.UP
                ? RotationSegment.convertToDegrees(blockEntity.getRotationSegment())
                : switch (state.facing) {
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.facing == Direction.UP) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            poseStack.translate(
                    0.5F - (float) state.facing.getStepX() * 0.25F,
                    0.25F,
                    0.5F - (float) state.facing.getStepZ() * 0.25F);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        model.resetPose();
        collector.submitModelPart(
                model.root(),
                poseStack,
                RenderTypes.entityCutoutNoCull(state.hellhound ? HELLHOUND_TEXTURE : WOLF_TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                null);
        poseStack.popPose();
    }
}
