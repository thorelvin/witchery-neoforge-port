package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity;
import com.emoniph.witchery.client.model.WitchesOvenModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class WitchesOvenRenderer implements BlockEntityRenderer<WitchesOvenBlockEntity, BlockEntityRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/witches_oven.png");
    private final WitchesOvenModel model;

    public WitchesOvenRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new WitchesOvenModel(ctx.bakeLayer(WitchesOvenModel.LAYER_LOCATION));
    }

    @Override
    public BlockEntityRenderState createRenderState() { return new BlockEntityRenderState(); }

    @Override
    public void submit(BlockEntityRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }
}
