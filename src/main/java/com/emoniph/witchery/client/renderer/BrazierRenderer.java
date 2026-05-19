package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.BrazierBlockEntity;
import com.emoniph.witchery.client.model.BrazierModel;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
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

public class BrazierRenderer implements BlockEntityRenderer<BrazierBlockEntity, BrazierRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/brazier.png");
    private final BrazierModel model;

    public static class State extends BlockEntityRenderState {
        public boolean renderAsh;
    }

    public BrazierRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new BrazierModel(ctx.bakeLayer(BrazierModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(BrazierBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.renderAsh = be.isBurning();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        if (state.renderAsh) {
            collector.submitModelPart(model.ashPart(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        ps.popPose();
    }
}
