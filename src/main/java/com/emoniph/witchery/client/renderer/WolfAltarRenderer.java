package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.WolfAltarBlock;
import com.emoniph.witchery.blocks.entity.WolfAltarBlockEntity;
import com.emoniph.witchery.client.model.WolfAltarModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the Wolf Altar as the source wolf-god statue (decompiled {@code RenderStatueWerewolf}).
 * The base transform matches the other ported statues (centre, flip, face); the three figures are then
 * submitted with the source's independent scaling — the upright wolfman at ×0.7 and each flanking wolf
 * at ×0.5 about its own pivot — while the plinth slabs draw at ×1.
 */
public class WolfAltarRenderer implements BlockEntityRenderer<WolfAltarBlockEntity, WolfAltarRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/wolfaltar.png");
    private final WolfAltarModel model;

    public WolfAltarRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new WolfAltarModel(ctx.bakeLayer(WolfAltarModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(WolfAltarBlockEntity be, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);
        var blockState = be.getBlockState();
        if (blockState.hasProperty(WolfAltarBlock.FACING)) {
            state.facing = blockState.getValue(WolfAltarBlock.FACING);
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType renderType = RenderTypes.entityCutout(TEXTURE);
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));

        // Upright wolfman ×0.7 about its body pivot.
        ps.pushPose();
        WolfAltarModel.scaleAboutPivot(ps, 0.0F, 3.1F, 0.0F, 0.7F);
        collector.submitModelPart(model.bipedBody(), ps, renderType, light, overlay, null);
        ps.popPose();

        // Plinth slabs at full scale.
        collector.submitModelPart(model.shape37(), ps, renderType, light, overlay, null);
        collector.submitModelPart(model.shape1_2(), ps, renderType, light, overlay, null);
        collector.submitModelPart(model.shape1(), ps, renderType, light, overlay, null);
        collector.submitModelPart(model.shape1_1(), ps, renderType, light, overlay, null);

        // Two flanking wolves ×0.5 about their pivots.
        ps.pushPose();
        WolfAltarModel.scaleAboutPivot(ps, -5.0F, 14.0F, 3.5F, 0.5F);
        collector.submitModelPart(model.wolfBody(), ps, renderType, light, overlay, null);
        ps.popPose();

        ps.pushPose();
        WolfAltarModel.scaleAboutPivot(ps, 5.5F, 14.0F, -2.0F, 0.5F);
        collector.submitModelPart(model.wolfBody1(), ps, renderType, light, overlay, null);
        ps.popPose();

        ps.popPose();
    }
}
