package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.CrystalBallBlockEntity;
import com.emoniph.witchery.client.model.CrystalBallModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public class CrystalBallRenderer
        implements BlockEntityRenderer<CrystalBallBlockEntity, CrystalBallRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/crystalball.png");
    private static final int OUTER_GLOBE_COLOR =
            ARGB.colorFromFloat(1.0F, 0.8F, 0.8F, 1.0F);

    private final CrystalBallModel model;

    public CrystalBallRenderer(BlockEntityRendererProvider.Context context) {
        model = new CrystalBallModel(context.bakeLayer(CrystalBallModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public int innerColor = innerGlobeColor(1.0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrystalBallBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        long gameTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime();
        state.innerColor = innerGlobeColor(sourceBrightness(gameTime));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        applySourcePose(poseStack);
        submitModel(model, poseStack, collector, state.lightCoords, state.innerColor);
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
    }

    static float sourceBrightness(long gameTime) {
        long phase = Math.floorMod(gameTime, 160L);
        return (100L - Math.abs(phase - 80L)) * 0.01F;
    }

    static int innerGlobeColor(float brightness) {
        return ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
    }

    public static void submitModel(CrystalBallModel model, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords, int innerColor) {
        for (ModelPart part : model.baseParts()) {
            collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        submitGlobePart(model.globeInner(), poseStack, collector, lightCoords, innerColor);
        submitGlobePart(model.globeMiddle(), poseStack, collector, lightCoords, OUTER_GLOBE_COLOR);
        submitGlobePart(model.globeOuter(), poseStack, collector, lightCoords, OUTER_GLOBE_COLOR);
    }

    private static void submitGlobePart(ModelPart part, PoseStack poseStack,
                                        SubmitNodeCollector collector, int lightCoords, int color) {
        collector.submitModelPart(part, poseStack, RenderTypes.entityTranslucent(TEXTURE),
                lightCoords, OverlayTexture.NO_OVERLAY, null, color, null);
    }
}
