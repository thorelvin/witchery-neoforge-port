package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.MirrorBlock;
import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.client.model.MirrorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MirrorBlockEntityRenderer
        implements BlockEntityRenderer<MirrorBlockEntity, MirrorBlockEntityRenderer.State> {
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/mirror.png");
    static final Identifier OCCUPIED_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/mirror2.png");

    private final MirrorModel model;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        model = new MirrorModel(context.bakeLayer(MirrorModel.LAYER_LOCATION));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean bottomOccupied;
        public boolean topOccupied;
        public int topLightCoords;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MirrorBlockEntity blockEntity, State state,
                                   float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        state.facing = blockEntity.getBlockState().getValue(MirrorBlock.FACING);
        state.bottomOccupied = blockEntity.isBottomOccupied();
        state.topOccupied = blockEntity.isTopOccupied();
        state.topLightCoords = blockEntity.getLevel() == null
                ? state.lightCoords
                : LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        submitHalf(state, poseStack, collector, false, state.bottomOccupied, state.lightCoords);
        submitHalf(state, poseStack, collector, true, state.topOccupied, state.topLightCoords);
    }

    @Override
    public AABB getRenderBoundingBox(MirrorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0.0D, 1.0D, 0.0D);
    }

    private void submitHalf(State state, PoseStack poseStack, SubmitNodeCollector collector,
                            boolean top, boolean occupied, int lightCoords) {
        poseStack.pushPose();
        if (top) {
            poseStack.translate(0.0F, 1.0F, 0.0F);
        }
        applySourcePose(poseStack, state.facing, top);
        for (var part : model.sourceParts()) {
            collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        if (occupied) {
            collector.submitModelPart(model.backMiddle(), poseStack,
                    RenderTypes.entityTranslucent(OCCUPIED_TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack, Direction facing, boolean top) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
        if (!top) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        poseStack.translate(0.0F, 0.1F, 0.42F);
        poseStack.translate(0.0F, -0.1F, 0.02F);
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> throw new IllegalArgumentException("Mirrors require a horizontal facing");
        };
    }
}
