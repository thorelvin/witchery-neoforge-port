package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.GarlicGarlandBlock;
import com.emoniph.witchery.blocks.entity.GarlicGarlandBlockEntity;
import com.emoniph.witchery.client.model.GarlicGarlandModel;
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
import net.minecraft.world.phys.Vec3;

public final class GarlicGarlandRenderer
        implements BlockEntityRenderer<GarlicGarlandBlockEntity, GarlicGarlandRenderer.State> {
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/garlic_garland.png");

    private final GarlicGarlandModel model;

    public GarlicGarlandRenderer(BlockEntityRendererProvider.Context context) {
        model = new GarlicGarlandModel(context.bakeLayer(GarlicGarlandModel.LAYER_LOCATION));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GarlicGarlandBlockEntity blockEntity, State state,
                                   float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        state.facing = blockEntity.getBlockState().getValue(GarlicGarlandBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        applySourcePose(poseStack, state.facing);
        collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.9F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
        poseStack.translate(0.0F, -0.1F, 0.02F);
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> throw new IllegalArgumentException("Garlic Garlands require a horizontal facing");
        };
    }
}
