package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.StatueGoddessBlock;
import com.emoniph.witchery.blocks.entity.StatueGoddessBlockEntity;
import com.emoniph.witchery.client.model.StatueGoddessModel;
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

public class StatueGoddessRenderer implements BlockEntityRenderer<StatueGoddessBlockEntity, StatueGoddessRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/statuegoddess.png");
    private final StatueGoddessModel model;

    public StatueGoddessRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new StatueGoddessModel(ctx.bakeLayer(StatueGoddessModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(StatueGoddessBlockEntity be, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);
        var blockState = be.getBlockState();
        if (blockState.hasProperty(StatueGoddessBlock.FACING)) {
            state.facing = blockState.getValue(StatueGoddessBlock.FACING);
        }
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
