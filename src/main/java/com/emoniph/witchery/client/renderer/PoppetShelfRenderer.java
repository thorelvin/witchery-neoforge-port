package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.client.model.PoppetShelfModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import com.mojang.math.Axis;

public class PoppetShelfRenderer implements BlockEntityRenderer<PoppetShelfBlockEntity, BlockEntityRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "witchery", "textures/block/poppet_shelf.png");
    private final PoppetShelfModel model;

    public PoppetShelfRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new PoppetShelfModel(ctx.bakeLayer(PoppetShelfModel.LAYER_LOCATION));
    }

    @Override
    public BlockEntityRenderState createRenderState() { return new BlockEntityRenderState(); }

    @Override
    public void submit(BlockEntityRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(-0.5, 0, -0.5);
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }
}
