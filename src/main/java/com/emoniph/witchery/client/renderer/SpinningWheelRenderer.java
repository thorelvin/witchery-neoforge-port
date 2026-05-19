package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.SpinningWheelBlock;
import com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity;
import com.emoniph.witchery.client.model.SpinningWheelModel;
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
import org.jspecify.annotations.Nullable;

public class SpinningWheelRenderer implements BlockEntityRenderer<SpinningWheelBlockEntity, SpinningWheelRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/spinning_wheel.png");
    private final SpinningWheelModel model;

    public static class State extends BlockEntityRenderState {
        public float yaw;
    }

    public SpinningWheelRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new SpinningWheelModel(ctx.bakeLayer(SpinningWheelModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(SpinningWheelBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        Direction facing = be.getBlockState().getValue(SpinningWheelBlock.FACING);
        state.yaw = facing.getOpposite().toYRot();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(state.yaw));
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }
}
