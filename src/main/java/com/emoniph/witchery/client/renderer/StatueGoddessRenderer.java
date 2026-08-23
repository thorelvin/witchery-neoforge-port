package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.StatueGoddessBlock;
import com.emoniph.witchery.blocks.entity.StatueGoddessBlockEntity;
import com.emoniph.witchery.client.model.StatueGoddessModel;
import com.mojang.blaze3d.vertex.PoseStack;
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
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;

public class StatueGoddessRenderer implements BlockEntityRenderer<StatueGoddessBlockEntity, StatueGoddessRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/statuegoddess.png");
    private final StatueGoddessModel model;

    public StatueGoddessRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new StatueGoddessModel(ctx.bakeLayer(StatueGoddessModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(StatueGoddessBlockEntity be, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);
        var blockState = be.getBlockState();
        if (blockState.hasProperty(StatueGoddessBlock.FACING)) {
            state.facing = blockState.getValue(StatueGoddessBlock.FACING);
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        applySourceWorldPose(ps, state.facing);
        model.setupSourcePose();
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }

    static void applySourceWorldPose(PoseStack ps, Direction facing) {
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
    }

    public static void applySourceItemPose(PoseStack ps) {
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
        ps.translate(0.0F, -1.0F, 0.0F);
        ps.scale(0.6F, 0.6F, 0.6F);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
        ps.translate(0.0F, 1.5F, 0.0F);
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> throw new IllegalArgumentException("Goddess statues require a horizontal facing");
        };
    }
}
