package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.entity.ChaliceBlockEntity;
import com.emoniph.witchery.client.model.ChaliceModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
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
    private static final Identifier WATER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/water_still.png");
    private static final float LIQUID_HALF_WIDTH = 1.35f / 16.0f;
    private static final float LIQUID_HALF_DEPTH = 2.35f / 16.0f;
    private static final float LIQUID_SURFACE_Y = 10.1f / 16.0f;
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
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutoutNoCull(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        if (state.filled) {
            renderWaterSurface(ps, collector, state.lightCoords);
        }
        ps.popPose();
    }

    private void renderWaterSurface(PoseStack ps, SubmitNodeCollector collector, int light) {
        ps.pushPose();
        ps.translate(0.0, LIQUID_SURFACE_Y, 0.0);
        collector.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WATER_TEXTURE), (pose, vc) -> {
            Matrix4f m = pose.pose();
            vc.addVertex(m, -LIQUID_HALF_WIDTH, 0, -LIQUID_HALF_DEPTH)
                    .setColor(0.247f, 0.463f, 0.894f, 0.9f)
                    .setUv(0.0f, 0.0f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(0, 1, 0);
            vc.addVertex(m, -LIQUID_HALF_WIDTH, 0, LIQUID_HALF_DEPTH)
                    .setColor(0.247f, 0.463f, 0.894f, 0.9f)
                    .setUv(0.0f, 1.0f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(0, 1, 0);
            vc.addVertex(m, LIQUID_HALF_WIDTH, 0, LIQUID_HALF_DEPTH)
                    .setColor(0.247f, 0.463f, 0.894f, 0.9f)
                    .setUv(1.0f, 1.0f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(0, 1, 0);
            vc.addVertex(m, LIQUID_HALF_WIDTH, 0, -LIQUID_HALF_DEPTH)
                    .setColor(0.247f, 0.463f, 0.894f, 0.9f)
                    .setUv(1.0f, 0.0f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(0, 1, 0);
        });
        ps.popPose();
    }
}
