package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.WolfAltarBlock;
import com.emoniph.witchery.blocks.entity.WolfAltarBlockEntity;
import com.emoniph.witchery.client.model.WolfAltarModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class WolfAltarRenderer implements BlockEntityRenderer<WolfAltarBlockEntity, WolfAltarRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/wolfaltar.png");
    private final WolfAltarModel model;

    public WolfAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new WolfAltarModel(context.bakeLayer(WolfAltarModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WolfAltarBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        var blockState = blockEntity.getBlockState();
        if (blockState.hasProperty(WolfAltarBlock.FACING)) {
            state.facing = blockState.getValue(WolfAltarBlock.FACING);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        applySourceWorldPose(poseStack, state.facing);
        submitModel(model, poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    static void applySourceWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
    }

    public static void applySourceItemPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.scale(0.6F, 0.6F, 0.6F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 1.5F, 0.0F);
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> throw new IllegalArgumentException("Wolf altars require a horizontal facing");
        };
    }

    public static void submitModel(WolfAltarModel model, PoseStack poseStack,
                                   SubmitNodeCollector collector, int light, int overlay) {
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);

        poseStack.pushPose();
        WolfAltarModel.scaleAboutPivot(poseStack, 0.0F, 3.1F, 0.0F, 0.7F);
        collector.submitModelPart(model.bipedBody(), poseStack, renderType, light, overlay, null);
        poseStack.popPose();

        collector.submitModelPart(model.shape37(), poseStack, renderType, light, overlay, null);
        collector.submitModelPart(model.shape1_2(), poseStack, renderType, light, overlay, null);
        collector.submitModelPart(model.shape1(), poseStack, renderType, light, overlay, null);

        poseStack.pushPose();
        WolfAltarModel.scaleAboutPivot(poseStack, -5.0F, 14.0F, 3.5F, 0.5F);
        collector.submitModelPart(model.wolfBody(), poseStack, renderType, light, overlay, null);
        poseStack.popPose();

        collector.submitModelPart(model.shape1_1(), poseStack, renderType, light, overlay, null);

        poseStack.pushPose();
        WolfAltarModel.scaleAboutPivot(poseStack, 5.5F, 14.0F, -2.0F, 0.5F);
        collector.submitModelPart(model.wolfBody1(), poseStack, renderType, light, overlay, null);
        poseStack.popPose();
    }
}
