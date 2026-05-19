package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.LeechChestBlock;
import com.emoniph.witchery.blocks.entity.LeechChestBlockEntity;
import com.emoniph.witchery.client.model.LeechChestModel;
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
import org.jspecify.annotations.Nullable;

public class LeechChestRenderer implements BlockEntityRenderer<LeechChestBlockEntity, LeechChestRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/leechchest.png");
    private final LeechChestModel model;

    public static class State extends BlockEntityRenderState {
        public float yaw;
    }

    public LeechChestRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new LeechChestModel(ctx.bakeLayer(LeechChestModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(LeechChestBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        Direction facing = be.getBlockState().getValue(LeechChestBlock.FACING);
        state.yaw = facing.toYRot();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0, 1, 1);
        ps.scale(1.0f, -1.0f, -1.0f);
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(state.yaw));
        ps.translate(-0.5, -0.5, -0.5);
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }
}
