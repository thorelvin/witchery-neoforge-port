package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.CoffinBlock;
import com.emoniph.witchery.blocks.entity.CoffinBlockEntity;
import com.emoniph.witchery.client.model.CoffinModel;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CoffinRenderer implements BlockEntityRenderer<CoffinBlockEntity, CoffinRenderer.CoffinRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/coffin.png");

    private final CoffinModel model;

    public static class CoffinRenderState extends BlockEntityRenderState {
        public float openAmount = 0f;
        public float facingYaw = 0f;
        public boolean shouldRender = true;
    }

    public CoffinRenderer(BlockEntityRendererProvider.Context ctx) {
        model = new CoffinModel(ctx.bakeLayer(CoffinModel.LAYER_LOCATION));
    }

    @Override
    public CoffinRenderState createRenderState() { return new CoffinRenderState(); }

    @Override
    public void extractRenderState(CoffinBlockEntity be, CoffinRenderState state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        if (be.getLevel() == null) { state.shouldRender = false; return; }
        BlockState bs = be.getBlockState();
        if (!(bs.getBlock() instanceof CoffinBlock)) { state.shouldRender = false; return; }
        state.shouldRender = true;
        state.openAmount = bs.getValue(CoffinBlock.OPEN) ? 1.0f : 0.0f;
        float yaw = facingToYaw(bs.getValue(CoffinBlock.FACING));
        if (bs.getValue(CoffinBlock.PART) == BedPart.FOOT) {
            yaw += 180.0f;
        }
        state.facingYaw = yaw;
    }

    @Override
    public void submit(CoffinRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.shouldRender) return;
        model.setOpen(state.openAmount);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -0.1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(state.facingYaw));
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();
    }

    private static float facingToYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0f;
            case WEST  -> 90f;
            case NORTH -> 180f;
            case EAST  -> 270f;
            default    -> 0f;
        };
    }
}
