package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.CoffinBlock;
import com.emoniph.witchery.blocks.entity.CoffinBlockEntity;
import com.emoniph.witchery.client.model.CoffinModel;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CoffinRenderer implements BlockEntityRenderer<CoffinBlockEntity, CoffinRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/coffin.png");

    private final CoffinModel model;

    public static class State extends BlockEntityRenderState {
        public float openAmount;
        public Direction facing = Direction.SOUTH;
        public BedPart part = BedPart.FOOT;
        public boolean shouldRender = true;
    }

    public CoffinRenderer(BlockEntityRendererProvider.Context context) {
        model = new CoffinModel(context.bakeLayer(CoffinModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CoffinBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        BlockState blockState = blockEntity.getBlockState();
        state.shouldRender = blockState.getBlock() instanceof CoffinBlock;
        if (!state.shouldRender) {
            return;
        }
        state.facing = blockState.getValue(CoffinBlock.FACING);
        state.part = blockState.getValue(CoffinBlock.PART);
        state.openAmount = blockEntity.getLidAngle(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        if (!state.shouldRender) {
            return;
        }
        poseStack.pushPose();
        applySourcePose(poseStack, state.facing);
        submitModel(model, poseStack, collector, state.lightCoords, state.part, state.openAmount);
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -0.1F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> throw new IllegalArgumentException("Coffins require a horizontal facing");
        };
    }

    public static void submitModel(CoffinModel model, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords,
                                   BedPart part, float openAmount) {
        model.setupSourcePose(part, openAmount);
        for (ModelPart modelPart : model.renderParts()) {
            collector.submitModelPart(modelPart, poseStack, RenderTypes.entityCutout(TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
    }
}
