package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.CoffinBlock;
import com.emoniph.witchery.blocks.entity.CoffinBlockEntity;
import com.emoniph.witchery.client.model.CoffinModel;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CoffinRenderer implements BlockEntityRenderer<CoffinBlockEntity, CoffinRenderer.CoffinRenderState> {
    private static final Identifier HEAD_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/coffin_head.png");
    private static final Identifier BOTTOM_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/coffin_bottom.png");

    private final CoffinModel headModel;
    private final CoffinModel bottomModel;

    public static class CoffinRenderState extends BlockEntityRenderState {
        public float openAmount = 0f;
        public float facingYaw = 0f;
        public boolean shouldRender = true;
    }

    public CoffinRenderer(BlockEntityRendererProvider.Context ctx) {
        headModel = new CoffinModel(ctx.bakeLayer(CoffinModel.HEAD_LAYER_LOCATION));
        bottomModel = new CoffinModel(ctx.bakeLayer(CoffinModel.BOTTOM_LAYER_LOCATION));
    }

    @Override
    public CoffinRenderState createRenderState() {
        return new CoffinRenderState();
    }

    @Override
    public void extractRenderState(CoffinBlockEntity be, CoffinRenderState state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        if (be.getLevel() == null) {
            state.shouldRender = false;
            return;
        }

        BlockState blockState = be.getBlockState();
        if (!(blockState.getBlock() instanceof CoffinBlock) || blockState.getValue(CoffinBlock.PART) != BedPart.HEAD) {
            state.shouldRender = false;
            return;
        }

        state.shouldRender = true;
        state.openAmount = blockState.getValue(CoffinBlock.OPEN) ? 1.0F : 0.0F;
        state.facingYaw = facingToYaw(blockState.getValue(CoffinBlock.FACING));
    }

    @Override
    public void submit(CoffinRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.shouldRender) {
            return;
        }

        headModel.setOpen(state.openAmount);
        bottomModel.setOpen(state.openAmount);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facingYaw));

        collector.submitModelPart(headModel.root(), poseStack, RenderTypes.entityCutout(HEAD_TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null);

        poseStack.translate(0.0F, 0.0F, -1.0F);
        collector.submitModelPart(bottomModel.root(), poseStack, RenderTypes.entityCutout(BOTTOM_TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }

    private static float facingToYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }
}
