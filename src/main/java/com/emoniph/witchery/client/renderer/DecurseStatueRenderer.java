package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.AreaMarkerBlock;
import com.emoniph.witchery.blocks.entity.AreaMarkerBlockEntity;
import com.emoniph.witchery.client.model.MandrakeStatueModel;
import com.emoniph.witchery.client.model.WolfStatueModel;
import com.emoniph.witchery.init.WitcheryBlocks;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class DecurseStatueRenderer
        implements BlockEntityRenderer<AreaMarkerBlockEntity, DecurseStatueRenderer.State> {
    private static final Identifier MANDRAKE_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/statuemandrake.png");
    private static final Identifier WOLF_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/statuewolf.png");

    private final MandrakeStatueModel mandrakeModel;
    private final WolfStatueModel wolfModel;

    public DecurseStatueRenderer(BlockEntityRendererProvider.Context context) {
        mandrakeModel = new MandrakeStatueModel(context.bakeLayer(MandrakeStatueModel.LAYER_LOCATION));
        wolfModel = new WolfStatueModel(context.bakeLayer(WolfStatueModel.LAYER_LOCATION));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean mandrake;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AreaMarkerBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        var blockState = blockEntity.getBlockState();
        state.facing = blockState.hasProperty(AreaMarkerBlock.FACING)
                ? blockState.getValue(AreaMarkerBlock.FACING)
                : Direction.NORTH;
        state.mandrake = blockState.is(WitcheryBlocks.DECURSE_TELEPORT.get());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        ModelPart model = state.mandrake ? mandrakeModel.root() : wolfModel.root();
        Identifier texture = state.mandrake ? MANDRAKE_TEXTURE : WOLF_TEXTURE;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, state.mandrake ? -0.255F : -0.26F, 0.0F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facing.toYRot() + 180.0F));
        collector.submitModelPart(model, poseStack, RenderTypes.entityCutout(texture),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }
}
