package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.PlacedModonomiconBlockEntity;
import com.emoniph.witchery.client.PlacedModonomiconPages;
import com.emoniph.witchery.client.model.PlacedModonomiconBookModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlacedModonomiconRenderer implements BlockEntityRenderer<PlacedModonomiconBlockEntity, PlacedModonomiconRenderer.State> {
    private static final Identifier BOOK_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/placed_modonomicon_book.png");
    private static final float HEXEREI_DEGREES_TO_RADIANS_DIVISOR = 57.3F;
    private static final float SURFACE_REST_Y = 0.165F;
    private static final float OPEN_BOOK_LIFT_Y = 0.215F;
    private static final float CLOSED_CENTER_X_SHIFT = -0.28125F;
    private static final float CLOSED_CENTER_Z_SHIFT = 0.125F;
    private static final float MODEL_BASIS_Z_ROTATION = 90.0F;
    private static final float MODEL_BASIS_X_ROTATION = 90.0F;
    private static final int MIN_BOOK_BLOCK_LIGHT = 5;

    private final PlacedModonomiconBookModel model;

    public static class State extends BlockEntityRenderState {
        public boolean hasBook;
        public float degreesSpun;
        public float degreesOpened;
        public float degreesFlopped;
        public float pageOneRotation;
        public float pageTwoRotation;
        public int openPagesIndex;
        public int turnPage;
        public int openingPageTurnsRemaining;
        public ItemStack bookStack = ItemStack.EMPTY;
    }

    public PlacedModonomiconRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new PlacedModonomiconBookModel(ctx.bakeLayer(PlacedModonomiconBookModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlacedModonomiconBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.hasBook = !be.getBookStack().isEmpty();
        state.bookStack = state.hasBook ? be.getBookStack().copy() : ItemStack.EMPTY;
        state.degreesSpun = lerpAngle(be.prevDegreesSpun, be.degreesSpun, partialTick);
        state.degreesOpened = PlacedModonomiconBlockEntity.easeOpened(
                Mth.lerp(partialTick, be.prevOpenedPercent, be.openedPercent)) * 90.0F;
        state.degreesFlopped = PlacedModonomiconBlockEntity.easeFlop(
                Mth.lerp(partialTick, be.prevFloppedPercent, be.floppedPercent)) * 90.0F;
        state.pageOneRotation = Mth.lerp(partialTick, be.prevPageOneRotation, be.pageOneRotation);
        state.pageTwoRotation = Mth.lerp(partialTick, be.prevPageTwoRotation, be.pageTwoRotation);
        state.openPagesIndex = be.getOpenPagesIndex();
        state.turnPage = be.turnPage;
        state.openingPageTurnsRemaining = be.openingPageTurnsRemaining;
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hasBook) {
            return;
        }

        float openAmount = Mth.clamp(1.0F - state.degreesOpened / 90.0F, 0.0F, 1.0F);
        float floppedAmount = Mth.clamp(state.degreesFlopped / 90.0F, 0.0F, 1.0F);
        float pageFlip = Mth.clamp(state.pageOneRotation / 180.0F, 0.0F, 1.0F);
        float backPageFlip = Mth.clamp(state.pageTwoRotation / 180.0F, 0.0F, 1.0F);
        boolean turningNextPage = state.turnPage == 1 || (state.pageOneRotation > 0.0F && state.pageOneRotation < 180.0F);
        boolean turningPreviousPage = state.turnPage == 2 || (state.pageTwoRotation > 0.0F && state.pageTwoRotation < 180.0F);
        boolean turningPage = turningNextPage || turningPreviousPage;
        float openShift = openAmount * (state.degreesOpened / 5.0F - 12.0F) / 32.0F;
        float openLift = Math.max(openAmount, 1.0F - floppedAmount) * OPEN_BOOK_LIFT_Y;
        float closedCenterAmount = (1.0F - openAmount) * floppedAmount;

        ps.pushPose();
        ps.translate(0.5F, SURFACE_REST_Y + openLift, 0.5F);
        ps.translate(
                Mth.sin(state.degreesSpun / HEXEREI_DEGREES_TO_RADIANS_DIVISOR) * openShift,
                (PlacedModonomiconBlockEntity.easeFlop(1.0F - floppedAmount) - 1.0F) / 16.0F,
                Mth.cos(state.degreesSpun / HEXEREI_DEGREES_TO_RADIANS_DIVISOR) * openShift);
        ps.mulPose(Axis.YP.rotationDegrees(state.degreesSpun));
        ps.translate(
                CLOSED_CENTER_X_SHIFT * closedCenterAmount,
                0.0F,
                CLOSED_CENTER_Z_SHIFT * closedCenterAmount);
        ps.mulPose(Axis.XP.rotationDegrees(-(state.degreesOpened / 2.0F + 45.0F)));
        ps.mulPose(Axis.YP.rotationDegrees(-state.degreesFlopped));
        ps.translate(
                0.0F,
                -0.00390625F * (1.0F - state.degreesOpened / 90.0F),
                -(state.degreesFlopped / 10.0F) / 32.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(MODEL_BASIS_Z_ROTATION));
        ps.mulPose(Axis.XP.rotationDegrees(MODEL_BASIS_X_ROTATION));
        ps.scale(0.8F, 0.8F, 0.8F);

        model.resetPose();
        model.setFlipPagesVisible(turningNextPage, turningPreviousPage);
        model.setupAnim(0.0F, pageFlip, backPageFlip, openAmount);
        int bookLight = hasReadingGlow(openAmount, floppedAmount)
                ? withMinimumBlockLight(state.lightCoords, MIN_BOOK_BLOCK_LIGHT)
                : state.lightCoords;
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutoutNoCull(BOOK_TEXTURE),
                bookLight, OverlayTexture.NO_OVERLAY, null);
        if (openAmount > 0.92F && state.openingPageTurnsRemaining <= 0 && !turningPage) {
            PlacedModonomiconPages.submit(state.bookStack, state.openPagesIndex, ps, collector, bookLight, openAmount);
        }
        ps.popPose();
    }

    private static int withMinimumBlockLight(int packedLight, int minimumBlockLight) {
        return LightTexture.pack(Math.max(minimumBlockLight, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
    }

    private static boolean hasReadingGlow(float openAmount, float floppedAmount) {
        return openAmount > 0.02F || floppedAmount < 0.98F;
    }

    private static float lerpAngle(float previous, float current, float partialTick) {
        return previous + Mth.wrapDegrees(current - previous) * partialTick;
    }
}
