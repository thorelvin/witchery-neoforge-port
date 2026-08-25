package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.SilverVatBlockEntity;
import com.emoniph.witchery.client.model.SilverVatModel;
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
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public class SilverVatRenderer
        implements BlockEntityRenderer<SilverVatBlockEntity, SilverVatRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/silvervat.png");
    private static final Identifier LIQUID_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/silvervat_liquid.png");
    private static final int LIQUID_COLOR =
            ARGB.colorFromFloat(1.0F, 52.0F / 256.0F, 95.0F / 256.0F, 218.0F / 256.0F);
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final SilverVatModel model;
    private final ModelPart liquid;

    public SilverVatRenderer(BlockEntityRendererProvider.Context context) {
        model = new SilverVatModel(context.bakeLayer(SilverVatModel.LAYER_LOCATION));
        liquid = context.bakeLayer(SilverVatModel.LIQUID_LAYER_LOCATION).getChild("liquid");
    }

    public static class State extends BlockEntityRenderState {
        public int depositParts;
        public boolean northSpout = true;
        public boolean southSpout = true;
        public boolean westSpout = true;
        public boolean eastSpout = true;

        public boolean rendersSpout(Direction direction) {
            return switch (direction) {
                case NORTH -> northSpout;
                case SOUTH -> southSpout;
                case WEST -> westSpout;
                case EAST -> eastSpout;
                default -> false;
            };
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SilverVatBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, overlay);
        state.depositParts = SilverVatBlockEntity.visibleDepositParts(blockEntity.getStored().getCount());

        var level = blockEntity.getLevel();
        var pos = blockEntity.getBlockPos();
        state.northSpout = rendersSpout(level == null,
                level != null && level.getBlockState(pos.north()).hasBlockEntity());
        state.southSpout = rendersSpout(level == null,
                level != null && level.getBlockState(pos.south()).hasBlockEntity());
        state.westSpout = rendersSpout(level == null,
                level != null && level.getBlockState(pos.west()).hasBlockEntity());
        state.eastSpout = rendersSpout(level == null,
                level != null && level.getBlockState(pos.east()).hasBlockEntity());
    }

    static boolean rendersSpout(boolean itemView, boolean neighborHasBlockEntity) {
        return itemView || neighborHasBlockEntity;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        applySourcePose(poseStack);

        for (ModelPart part : model.bodyParts()) {
            submitPart(part, poseStack, collector, state.lightCoords);
        }
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (state.rendersSpout(direction)) {
                for (ModelPart part : model.spout(direction)) {
                    submitPart(part, poseStack, collector, state.lightCoords);
                }
            }
        }
        for (int i = 0; i < state.depositParts; i++) {
            submitPart(model.deposits().get(i), poseStack, collector, state.lightCoords);
        }

        submitLiquid(liquid, poseStack, collector, state.lightCoords);
        poseStack.popPose();
    }

    static void applySourcePose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
    }

    private static void submitPart(ModelPart part, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords) {
        collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                lightCoords, OverlayTexture.NO_OVERLAY, null);
    }

    public static void submitLiquid(ModelPart liquid, PoseStack poseStack,
                                    SubmitNodeCollector collector, int lightCoords) {
        poseStack.pushPose();
        applyLiquidScale(poseStack);
        collector.submitModelPart(liquid, poseStack, RenderTypes.entityTranslucent(LIQUID_TEXTURE),
                lightCoords, OverlayTexture.NO_OVERLAY, null, LIQUID_COLOR, null);
        poseStack.popPose();
    }

    static void applyLiquidScale(PoseStack poseStack) {
        poseStack.scale(0.75F, 1.0F, 0.75F);
    }
}
