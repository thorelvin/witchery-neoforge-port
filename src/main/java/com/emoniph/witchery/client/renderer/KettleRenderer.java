package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.KettleBlock;
import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.emoniph.witchery.client.model.KettleModel;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class KettleRenderer implements BlockEntityRenderer<KettleBlockEntity, KettleRenderer.KettleRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/kettle.png");

    private final KettleModel model;

    public static class KettleRenderState extends BlockEntityRenderState {
        public int waterLevel = 0;
        public int color = 0;
        public int bottleCount = 0;
        public int liquidFrame = 0;
        public float liquidShade = 1.0F;
        public float yaw = 0.0F;
        public boolean brewing = false;
        public boolean ruined = false;
        public boolean renderCrossbar = true;
        public Level level = null;
        public BlockPos bePos = BlockPos.ZERO;
    }

    public KettleRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new KettleModel(ctx.bakeLayer(KettleModel.LAYER_LOCATION));
    }

    @Override
    public KettleRenderState createRenderState() {
        return new KettleRenderState();
    }

    @Override
    public void extractRenderState(KettleBlockEntity be, KettleRenderState state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(KettleBlock.FACING);
        state.yaw = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        state.waterLevel = be.getWaterLevel();
        state.color = be.getColor();
        state.brewing = be.isBrewing();
        state.ruined = be.isRuined();
        state.bottleCount = Math.min(2, be.bottleCount());
        state.level = be.getLevel();
        state.bePos = be.getBlockPos();
        state.liquidFrame = liquidFrame(state.level == null ? 0L : state.level.getGameTime());
        state.renderCrossbar = state.level == null
                || !state.level.getBlockState(state.bePos.above()).isFaceSturdy(state.level, state.bePos.above(), Direction.DOWN);
        state.liquidShade = state.brewing ? 0.5F : 1.0F;
    }

    @Override
    public void submit(KettleRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        applySourcePose(ps, state.yaw);

        for (var part : model.bodyParts()) {
            collector.submitModelPart(part, ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        if (state.renderCrossbar) {
            collector.submitModelPart(model.crossbar(), ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        for (var part : model.chainParts()) {
            collector.submitModelPart(part, ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        if (state.bottleCount > 0) {
            collector.submitModelPart(model.bottle1(), ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
            if (state.bottleCount > 1) {
                collector.submitModelPart(model.bottle2(), ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                        state.lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
        }
        if (state.waterLevel > 0) {
            ps.pushPose();
            ps.translate(0.0F, liquidYOffset(state.waterLevel), 0.0F);
            collector.submitModelPart(model.liquid(state.liquidFrame), ps, RenderTypes.entityTranslucent(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null, liquidColor(state), null);
            ps.popPose();
        }

        ps.popPose();

    }

    static void applySourcePose(PoseStack ps, float yaw) {
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
        ps.translate(0.0F, -1.0F, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    static int liquidFrame(long gameTime) {
        return (int) ((gameTime / 20L) & 3L);
    }

    static float liquidYOffset(int waterLevel) {
        return switch (Math.max(1, Math.min(KettleBlockEntity.MAX_WATER_LEVEL, waterLevel))) {
            case 1 -> 2.0F / 16.0F;
            case 2 -> 1.0F / 16.0F;
            default -> 0.0F;
        };
    }

    private static int liquidColor(KettleRenderState state) {
        int color = state.color;
        float shade = state.liquidShade;
        if (state.ruined) {
            color = 0x7F5C40;
            shade = 1.0F;
        } else if (color == 0) {
            color = 0x3757C3;
            shade = 1.0F;
        }
        float red = ((color >>> 16) & 0xFF) / 256.0F * shade;
        float green = ((color >>> 8) & 0xFF) / 256.0F * shade;
        float blue = (color & 0xFF) / 256.0F * shade;
        return ARGB.colorFromFloat(1.0F, red, green, blue);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
