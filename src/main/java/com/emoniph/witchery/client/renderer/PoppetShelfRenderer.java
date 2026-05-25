package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.client.model.PoppetShelfModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
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

    private final PoppetShelfModel model;
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] slotRenderStates = new ItemStackRenderState[PoppetShelfBlockEntity.SLOT_COUNT];
        public final float[] slotRotations = new float[PoppetShelfBlockEntity.SLOT_COUNT];
        public float yaw;

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
            state.yaw = 0f;
            clearSlots(state);
            return;
        }

        Direction facing = be.getBlockState().hasProperty(com.emoniph.witchery.blocks.PoppetShelfBlock.FACING)
                ? be.getBlockState().getValue(com.emoniph.witchery.blocks.PoppetShelfBlock.FACING)
                : Direction.NORTH;
        state.yaw = facing.getOpposite().toYRot();

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
            if (stack.isEmpty()) {
                state.slotRenderStates[slot] = new ItemStackRenderState();
                state.slotRotations[slot] = 0f;
                continue;
            }

            ItemStackRenderState renderState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(renderState, stack.copyWithCount(1), ItemDisplayContext.GROUND, level, owner, 0);
            state.slotRenderStates[slot] = renderState;
            state.slotRotations[slot] = (float) Math.floorMod(be.getBlockPos().hashCode() * 31 + slot * 47, 360);
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(state.yaw));
        ps.translate(-0.5, 0, -0.5);
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ps.popPose();

        for (int slot = 0; slot < state.slotRenderStates.length; slot++) {
            ItemStackRenderState renderState = state.slotRenderStates[slot];
            if (renderState == null || renderState.isEmpty()) {
                continue;
            }

            int row = slot / 3;
            int col = slot % 3;
            float x = 0.25f + col * 0.25f;
            float z = 0.25f + row * 0.25f;

            ps.pushPose();
            ps.translate(x, 0.6f, z);
            ps.scale(0.5f, 0.5f, 0.5f);
            ps.mulPose(Axis.YP.rotationDegrees(state.slotRotations[slot]));
            ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
            renderState.submit(ps, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
    }

    private static void clearSlots(State state) {
        for (int slot = 0; slot < state.slotRenderStates.length; slot++) {
            state.slotRenderStates[slot] = new ItemStackRenderState();
            state.slotRotations[slot] = 0f;
        }
    }
}
