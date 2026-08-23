package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.client.model.PoppetShelfModel;
import com.emoniph.witchery.item.PoppetItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PoppetShelfRenderer implements BlockEntityRenderer<PoppetShelfBlockEntity, PoppetShelfRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "witchery", "textures/block/poppet_shelf.png");
    private static final float FIRST_SLOT_CENTER = 0.25f;
    private static final float SLOT_SPACING = 0.25f;
    private static final float SLOT_Y = 0.6f;

    private final PoppetShelfModel model;
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] slotRenderStates = new ItemStackRenderState[PoppetShelfBlockEntity.SLOT_COUNT];
        public final float[] slotRotations = new float[PoppetShelfBlockEntity.SLOT_COUNT];
        public State() {
            for (int i = 0; i < slotRenderStates.length; i++) {
                slotRenderStates[i] = new ItemStackRenderState();
            }
        }
    }

    public PoppetShelfRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new PoppetShelfModel(ctx.bakeLayer(PoppetShelfModel.LAYER_LOCATION));
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PoppetShelfBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);

        Level level = be.getLevel();
        if (level == null) {
            clearSlots(state);
            return;
        }

        GraphicsPreset graphicsPreset = Minecraft.getInstance().options.graphicsPreset().get();
        boolean spinStoredItems = graphicsPreset == GraphicsPreset.FANCY || graphicsPreset == GraphicsPreset.FABULOUS;
        float sourceSpin = spinStoredItems ? sourceSpinDegrees(System.currentTimeMillis()) : 0.0F;

        Vec3 bePos = Vec3.atCenterOf(be.getBlockPos());
        ItemOwner owner = new ItemOwner() {
            @Override
            public Level level() {
                return level;
            }

            @Override
            public Vec3 position() {
                return bePos;
            }

            @Override
            public float getVisualRotationYInDegrees() {
                return 0f;
            }
        };

        for (int slot = 0; slot < PoppetShelfBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = be.getItem(slot);
            if (!(stack.getItem() instanceof PoppetItem)) {
                state.slotRenderStates[slot] = new ItemStackRenderState();
                state.slotRotations[slot] = 0f;
                continue;
            }

            ItemStackRenderState renderState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(renderState, stack.copyWithCount(1), ItemDisplayContext.GROUND, level, owner, 0);
            state.slotRenderStates[slot] = renderState;
            state.slotRotations[slot] = sourceSpin;
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        applySourceShelfPose(ps);
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();

        for (int slot = 0; slot < state.slotRenderStates.length; slot++) {
            ItemStackRenderState renderState = state.slotRenderStates[slot];
            if (renderState == null || renderState.isEmpty()) {
                continue;
            }

            ps.pushPose();
            applyStoredPoppetPose(ps, slot, state.slotRotations[slot]);
            renderState.submit(ps, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
    }

    static void applySourceShelfPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    static void applyStoredPoppetPose(PoseStack poseStack, int slot, float spinDegrees) {
        int row = slot / 3;
        int column = slot % 3;
        poseStack.translate(FIRST_SLOT_CENTER + column * SLOT_SPACING, SLOT_Y,
                FIRST_SLOT_CENTER + row * SLOT_SPACING);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));
        // Modern GROUND item transforms turn flat sprites horizontal; the source EntityItem renderer kept them upright.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
    }

    static float sourceSpinDegrees(long elapsedMillis) {
        return (elapsedMillis / 20.0F) % 360.0F;
    }

    private static void clearSlots(State state) {
        for (int slot = 0; slot < state.slotRenderStates.length; slot++) {
            state.slotRenderStates[slot] = new ItemStackRenderState();
            state.slotRotations[slot] = 0f;
        }
    }
}
