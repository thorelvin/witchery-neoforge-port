package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.PlacedItemBlock;
import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlacedItemRenderer implements BlockEntityRenderer<PlacedItemBlockEntity, PlacedItemRenderer.PlacedItemRenderState> {

    private final ItemModelResolver itemModelResolver;

    public static class PlacedItemRenderState extends BlockEntityRenderState {
        public ItemStackRenderState itemRenderState = new ItemStackRenderState();
        public float rotation = 0f;
    }

    public PlacedItemRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public PlacedItemRenderState createRenderState() { return new PlacedItemRenderState(); }

    @Override
    public void extractRenderState(PlacedItemBlockEntity be, PlacedItemRenderState state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        ItemStack stack = be.getStack();
        if (!stack.isEmpty()) {
            net.minecraft.world.phys.Vec3 bePos = net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos());
            net.minecraft.world.level.Level beLevel = be.getLevel();
            itemModelResolver.updateForTopItem(state.itemRenderState, stack, ItemDisplayContext.GROUND, beLevel,
                new net.minecraft.world.entity.ItemOwner() {
                    public net.minecraft.world.level.Level level() { return beLevel; }
                    public net.minecraft.world.phys.Vec3 position() { return bePos; }
                    public float getVisualRotationYInDegrees() { return 0f; }
                }, 0);
        } else {
            state.itemRenderState = new ItemStackRenderState();
        }
        state.rotation = sourceRotation(be.getBlockState());
    }

    @Override
    public void submit(PlacedItemRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.itemRenderState.isEmpty()) return;
        ps.pushPose();
        ps.translate(0.5, 0.03, 0.5);   // just above altar surface (collider base is Y=0)
        ps.mulPose(Axis.YP.rotationDegrees(state.rotation));
        ps.mulPose(Axis.XP.rotationDegrees(-90.0F));  // lay flat
        ps.scale(1.5f, 1.5f, 1.5f);    // 3x previous size to fill collider footprint
        state.itemRenderState.submit(ps, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    private static float sourceRotation(BlockState state) {
        Direction facing = state.hasProperty(PlacedItemBlock.FACING)
                ? state.getValue(PlacedItemBlock.FACING)
                : Direction.SOUTH;
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }
}
