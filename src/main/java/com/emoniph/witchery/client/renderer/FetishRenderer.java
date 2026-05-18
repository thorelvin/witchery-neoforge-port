package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.FetishBlockEntity;
import com.emoniph.witchery.client.model.FetishModel;
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

public class FetishRenderer implements BlockEntityRenderer<FetishBlockEntity, FetishRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/scarecrow.png");
    private final FetishModel model;

    public FetishRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new FetishModel(ctx.bakeLayer(FetishModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(FetishBlockEntity be, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);
        state.facing = be.getFacing();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }
}
