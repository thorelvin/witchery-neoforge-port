package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.client.model.DreamCatcherModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DreamCatcherRenderer
        implements BlockEntityRenderer<DreamCatcherBlockEntity, DreamCatcherRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/dream_catcher.png");

    private final DreamCatcherModel model;

    public DreamCatcherRenderer(BlockEntityRendererProvider.Context context) {
        model = new DreamCatcherModel(context.bakeLayer(DreamCatcherModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public @Nullable DreamWeaveType weave;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DreamCatcherBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        state.facing = blockEntity.getBlockState().getValue(DreamCatcherBlock.FACING);
        state.weave = blockEntity.getWeave();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        applySourcePose(poseStack, state.facing);
        submitModel(model, poseStack, collector, state.lightCoords, state.weave);
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        switch (facing) {
            case NORTH -> poseStack.translate(0.0F, 0.0F, -0.01F);
            case SOUTH -> poseStack.translate(0.0F, 0.0F, 0.01F);
            case WEST -> poseStack.translate(0.01F, 0.0F, 0.0F);
            case EAST -> poseStack.translate(-0.01F, 0.0F, 0.0F);
            default -> throw new IllegalArgumentException("Dream Catchers require a horizontal facing");
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> throw new IllegalArgumentException("Dream Catchers require a horizontal facing");
        };
    }

    public static void submitModel(DreamCatcherModel model, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords,
                                   @Nullable DreamWeaveType weave) {
        for (ModelPart part : model.frameParts()) {
            submitPart(part, poseStack, collector, lightCoords);
        }
        if (weave != null) {
            submitPart(model.net(weave), poseStack, collector, lightCoords);
        }
        submitPart(model.decoration(), poseStack, collector, lightCoords);
    }

    private static void submitPart(ModelPart part, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords) {
        collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                lightCoords, OverlayTexture.NO_OVERLAY, null);
    }
}
