package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.FumeFunnelBlock;
import com.emoniph.witchery.blocks.entity.FumeFunnelBlockEntity;
import com.emoniph.witchery.client.model.FumeFunnelModel;
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

public class FumeFunnelRenderer implements BlockEntityRenderer<FumeFunnelBlockEntity, FumeFunnelRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/fumefunnel.png");
    private final FumeFunnelModel model;

    public static class State extends BlockEntityRenderState {
        public float yaw;
    }

    public FumeFunnelRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new FumeFunnelModel(ctx.bakeLayer(FumeFunnelModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(FumeFunnelBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        Direction facing = be.getBlockState().getValue(FumeFunnelBlock.FACING);
        state.yaw = facing.getOpposite().toYRot();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        // Static funnel geometry now comes from the resurrected block model path.
    }
}
