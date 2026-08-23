package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.entity.ChaliceBlockEntity;
import com.emoniph.witchery.client.model.ChaliceModel;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ChaliceRenderer implements BlockEntityRenderer<ChaliceBlockEntity, ChaliceRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/chalice.png");
    private final ChaliceModel model;

    public static class State extends BlockEntityRenderState {
        public boolean filled;
    }

    public ChaliceRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new ChaliceModel(ctx.bakeLayer(ChaliceModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(ChaliceBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.filled = be.getBlockState().getValue(ChaliceBlock.FILLED);
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        applySourcePose(ps);
        collector.submitModelPart(model.chalice(), ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        if (state.filled) {
            collector.submitModelPart(model.liquid(), ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        ps.popPose();
    }

    static void applySourcePose(PoseStack ps) {
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
    }
}
