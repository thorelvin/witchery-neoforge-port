package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.FetishBlockEntity;
import com.emoniph.witchery.client.model.FetishModel;
import com.emoniph.witchery.client.model.FetishTrentModel;
import com.emoniph.witchery.init.WitcheryBlocks;
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
import net.minecraft.world.phys.Vec3;

public class FetishRenderer implements BlockEntityRenderer<FetishBlockEntity, FetishRenderer.State> {

    private static final Identifier SCARECROW_TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/scarecrow.png");
    private static final Identifier TRENT_TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/trent.png");
    private final FetishModel scarecrowModel;
    private final FetishTrentModel trentModel;

    public FetishRenderer(BlockEntityRendererProvider.Context ctx) {
        this.scarecrowModel = new FetishModel(ctx.bakeLayer(FetishModel.LAYER_LOCATION));
        this.trentModel = new FetishTrentModel(ctx.bakeLayer(FetishTrentModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public FetishVariant variant = FetishVariant.SCARECROW;
    }

    private enum FetishVariant {
        SCARECROW,
        TRENT,
        LADDER
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(FetishBlockEntity be, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);
        state.facing = be.getFacing();
        state.variant = variantFor(be.getBlockState());
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.variant == FetishVariant.LADDER) {
            return;
        }
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        if (state.variant == FetishVariant.TRENT) {
            collector.submitModelPart(trentModel.root(), ps, RenderTypes.entityCutout(TRENT_TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        } else {
            collector.submitModelPart(scarecrowModel.root(), ps, RenderTypes.entityCutout(SCARECROW_TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        ps.popPose();
    }

    private static FetishVariant variantFor(BlockState state) {
        if (state.is(WitcheryBlocks.FETISH_TRENT_EFFIGY.get())) {
            return FetishVariant.TRENT;
        }
        if (state.is(WitcheryBlocks.FETISH_WITCHS_LADDER.get())) {
            return FetishVariant.LADDER;
        }
        return FetishVariant.SCARECROW;
    }
}
