package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.AlluringSkullBlock;
import com.emoniph.witchery.blocks.entity.AlluringSkullBlockEntity;
import com.emoniph.witchery.client.model.AlluringSkullModel;
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

public class AlluringSkullRenderer implements BlockEntityRenderer<AlluringSkullBlockEntity, AlluringSkullRenderer.State> {
    private static final Identifier TEXTURE_OFF =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/alluring_skull.png");
    private static final Identifier TEXTURE_ON =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/alluring_skull2.png");

    private final AlluringSkullModel model;

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.UP;
        public boolean on;
        public float yaw;
    }

    public AlluringSkullRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new AlluringSkullModel(context.bakeLayer(AlluringSkullModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AlluringSkullBlockEntity blockEntity, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.getValue(AlluringSkullBlock.FACING);
        state.on = blockState.getValue(AlluringSkullBlock.ON);
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
        applySourcePose(poseStack, state.facing, state.yaw);
        model.resetPose();
        collector.submitModelPart(
                model.root(),
                poseStack,
                RenderTypes.entityCutoutNoCull(state.on ? TEXTURE_ON : TEXTURE_OFF),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                null);
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack, Direction facing, float yaw) {
        if (facing == Direction.UP) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            switch (facing) {
                case NORTH -> poseStack.translate(0.5F, 0.25F, 0.74F);
                case SOUTH -> poseStack.translate(0.5F, 0.25F, 0.26F);
                case WEST -> poseStack.translate(0.74F, 0.25F, 0.5F);
                case EAST -> poseStack.translate(0.26F, 0.25F, 0.5F);
                default -> throw new IllegalArgumentException("Alluring skulls cannot face down");
            }
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
    }
}
