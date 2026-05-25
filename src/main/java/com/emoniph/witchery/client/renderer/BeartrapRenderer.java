package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.BeartrapBlock;
import com.emoniph.witchery.blocks.entity.BeartrapBlockEntity;
import com.emoniph.witchery.client.model.BeartrapModel;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BeartrapRenderer implements BlockEntityRenderer<BeartrapBlockEntity, BeartrapRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/beartrap.png");

    private final BeartrapModel model;

    public static class State extends BlockEntityRenderState {
        public boolean closed;
        public boolean hiddenToViewer;
        public float yaw;
    }

    public BeartrapRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new BeartrapModel(ctx.bakeLayer(BeartrapModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BeartrapBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        BlockState blockState = be.getBlockState();
        state.closed = blockState.getValue(BeartrapBlock.CLOSED);
        state.yaw = blockState.getValue(BeartrapBlock.AXIS) == Direction.Axis.Z ? 90.0F : 0.0F;
        Player viewer = Minecraft.getInstance().player;
        state.hiddenToViewer = state.closed
                && !blockState.is(WitcheryBlocks.WOLFTRAP.get())
                && be.hasOwner()
                && (viewer == null || !be.isOwner(viewer));
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
        ps.translate(0.0F, -1.0F, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(state.yaw));
        model.setClosed(state.closed);
        if (state.hiddenToViewer) {
            float alpha = WitcheryConfig.MANTRAP_OPACITY_PERCENT.get() / 100.0F;
            int color = ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
            collector.submitModelPart(model.root(), ps, RenderTypes.entityTranslucent(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null, color, null);
        } else {
            collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        ps.popPose();
    }
}
