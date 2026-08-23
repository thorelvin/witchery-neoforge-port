package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.FumeFunnelBlockEntity;
import com.emoniph.witchery.client.model.FumeFunnelModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FumeFunnelRenderer implements BlockEntityRenderer<FumeFunnelBlockEntity, FumeFunnelRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/fumefunnel.png");
    private final FumeFunnelModel model;

    public static class State extends BlockEntityRenderState {
        public float yaw;
        public boolean renderWideBody;
        public boolean renderLeftConnection;
        public boolean renderRightConnection;
        public boolean renderChimney;
        public boolean filtered;
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
        var facing = be.getBlockState().getValue(com.emoniph.witchery.blocks.FumeFunnelBlock.FACING);
        state.yaw = facing.getOpposite().toYRot();
        state.filtered = be.getBlockState().getBlock() instanceof com.emoniph.witchery.blocks.FumeFunnelBlock funnel
                && funnel.isFiltered();

        if (be.getLevel() == null) {
            applySourceVisibility(state, false, false, false);
            return;
        }

        var level = be.getLevel();
        var pos = be.getBlockPos();
        boolean chimney = level.getBlockState(pos.below()).getBlock() instanceof com.emoniph.witchery.blocks.WitchesOvenBlock;
        boolean leftConnection = isLinkedOven(level, switch (facing) {
            case NORTH -> pos.east();
            case SOUTH -> pos.west();
            case WEST -> pos.north();
            case EAST -> pos.south();
            default -> pos;
        });
        boolean rightConnection = isLinkedOven(level, switch (facing) {
            case NORTH -> pos.west();
            case SOUTH -> pos.east();
            case WEST -> pos.south();
            case EAST -> pos.north();
            default -> pos;
        });
        applySourceVisibility(state, chimney, leftConnection, rightConnection);
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        applySourcePose(ps, state.yaw);

        if (state.renderWideBody) {
            submitParts(ps, collector, state, model.wideBodyParts());
        }
        if (state.renderLeftConnection) {
            submitParts(ps, collector, state, model.leftConnectionParts());
        }
        if (state.renderRightConnection) {
            submitParts(ps, collector, state, model.rightConnectionParts());
        }
        if (state.filtered && state.renderWideBody) {
            submitParts(ps, collector, state, model.filterParts());
        }
        if (state.renderChimney) {
            submitParts(ps, collector, state, model.chimneyParts());
        }

        ps.popPose();
    }

    static void applySourcePose(PoseStack ps, float yaw) {
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    static void applySourceVisibility(State state, boolean chimney,
                                      boolean leftConnection, boolean rightConnection) {
        state.renderWideBody = !chimney;
        state.renderChimney = chimney;
        state.renderLeftConnection = leftConnection;
        state.renderRightConnection = rightConnection;
    }

    private void submitParts(PoseStack ps, SubmitNodeCollector collector, State state, Iterable<net.minecraft.client.model.geom.ModelPart> parts) {
        for (var part : parts) {
            collector.submitModelPart(part, ps, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
    }

    private static boolean isLinkedOven(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof com.emoniph.witchery.blocks.WitchesOvenBlock;
    }
}
