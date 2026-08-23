package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.FetishBlock;
import com.emoniph.witchery.blocks.entity.FetishBlockEntity;
import com.emoniph.witchery.client.model.FetishModel;
import com.emoniph.witchery.client.model.FetishTrentModel;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FetishRenderer implements BlockEntityRenderer<FetishBlockEntity, FetishRenderer.State> {
    private static final Identifier SCARECROW_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/scarecrow.png");
    private static final Identifier TRENT_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/trent.png");
    private static final float[][] FLEECE_COLORS = {
            {1.0F, 1.0F, 1.0F}, {0.85F, 0.5F, 0.2F}, {0.7F, 0.3F, 0.85F}, {0.4F, 0.6F, 0.85F},
            {0.9F, 0.9F, 0.2F}, {0.5F, 0.8F, 0.1F}, {0.95F, 0.5F, 0.65F}, {0.3F, 0.3F, 0.3F},
            {0.6F, 0.6F, 0.6F}, {0.3F, 0.5F, 0.6F}, {0.5F, 0.25F, 0.7F}, {0.2F, 0.3F, 0.7F},
            {0.4F, 0.3F, 0.2F}, {0.4F, 0.5F, 0.2F}, {0.6F, 0.2F, 0.2F}, {0.1F, 0.1F, 0.1F}
    };

    private final FetishModel scarecrowModel;
    private final FetishTrentModel trentModel;

    public FetishRenderer(BlockEntityRendererProvider.Context context) {
        scarecrowModel = new FetishModel(context.bakeLayer(FetishModel.LAYER_LOCATION));
        trentModel = new FetishTrentModel(context.bakeLayer(FetishTrentModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public FetishVariant variant = FetishVariant.SCARECROW;
        public int colorIndex;
        public boolean spectral;
    }

    enum FetishVariant {
        SCARECROW,
        TRENT,
        LADDER
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FetishBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.hasProperty(FetishBlock.FACING)
                ? blockState.getValue(FetishBlock.FACING) : Direction.NORTH;
        state.variant = variantFor(blockState);
        state.colorIndex = blockEntity.getColor();
        state.spectral = blockEntity.isSpectral();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        if (state.variant == FetishVariant.LADDER) {
            return;
        }
        poseStack.pushPose();
        applySourcePose(poseStack, state.facing);
        if (state.variant == FetishVariant.TRENT) {
            submitTrent(trentModel, poseStack, collector, state.lightCoords, state.colorIndex, state.spectral);
        } else {
            submitScarecrow(scarecrowModel, poseStack, collector,
                    state.lightCoords, state.colorIndex, state.spectral);
        }
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sourceRotation(facing)));
    }

    static float sourceRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> throw new IllegalArgumentException("Fetishes require a horizontal facing");
        };
    }

    public static void submitScarecrow(FetishModel model, PoseStack poseStack,
                                       SubmitNodeCollector collector, int lightCoords,
                                       int colorIndex, boolean spectral) {
        int baseColor = ARGB.colorFromFloat(spectral ? 0.6F : 1.0F, 1.0F, 1.0F, 1.0F);
        int clothColor = sourceTintColor(colorIndex, spectral ? 0.7F : 1.0F);
        submitParts(model.untintedParts(), poseStack, collector, lightCoords,
                SCARECROW_TEXTURE, spectral, baseColor);
        submitParts(model.tintedParts(), poseStack, collector, lightCoords,
                SCARECROW_TEXTURE, spectral, clothColor);
    }

    public static void submitTrent(FetishTrentModel model, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords,
                                   int colorIndex, boolean spectral) {
        int baseColor = ARGB.colorFromFloat(spectral ? 0.6F : 1.0F, 1.0F, 1.0F, 1.0F);
        submitParts(model.untintedParts(), poseStack, collector, lightCoords,
                TRENT_TEXTURE, spectral, baseColor);
        submitPart(model.face(), poseStack, collector, lightCoords,
                TRENT_TEXTURE, spectral, sourceTintColor(colorIndex, 1.0F));
    }

    static int sourceTintColor(int colorIndex, float alpha) {
        int safeIndex = colorIndex >= 0 && colorIndex < FLEECE_COLORS.length ? colorIndex : 9;
        float[] color = FLEECE_COLORS[safeIndex];
        return ARGB.colorFromFloat(alpha, color[0], color[1], color[2]);
    }

    private static void submitParts(List<ModelPart> parts, PoseStack poseStack,
                                    SubmitNodeCollector collector, int lightCoords,
                                    Identifier texture, boolean translucent, int color) {
        for (ModelPart part : parts) {
            submitPart(part, poseStack, collector, lightCoords, texture, translucent, color);
        }
    }

    private static void submitPart(ModelPart part, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords,
                                   Identifier texture, boolean translucent, int color) {
        RenderType renderType = translucent
                ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture);
        collector.submitModelPart(part, poseStack, renderType,
                lightCoords, OverlayTexture.NO_OVERLAY, null, color, null);
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
