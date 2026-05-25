package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.SunCollectorBlock;
import com.emoniph.witchery.blocks.entity.SunCollectorBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SunCollectorRenderer implements BlockEntityRenderer<SunCollectorBlockEntity, SunCollectorRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        public ItemStackRenderState itemRenderState = new ItemStackRenderState();
        public float rotation;
    }

    public SunCollectorRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SunCollectorBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);

        ItemStack stack = be.getStored();
        if (stack.isEmpty() && be.getBlockState().hasProperty(SunCollectorBlock.POWER)) {
            int power = be.getBlockState().getValue(SunCollectorBlock.POWER);
            if (power >= 15) {
                stack = WitcheryItems.CHARGED_QUARTZ_SPHERE.get().getDefaultInstance();
            } else if (power > 0) {
                stack = WitcheryItems.QUARTZ_SPHERE.get().getDefaultInstance();
            }
        }
        Level level = be.getLevel();
        if (stack.isEmpty() || level == null) {
            state.itemRenderState = new ItemStackRenderState();
            state.rotation = 0f;
            return;
        }

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
        itemModelResolver.updateForTopItem(state.itemRenderState, stack.copyWithCount(1),
                ItemDisplayContext.GROUND, level, owner, 0);
        state.rotation = ((level.getGameTime() + partialTick) * 2.5f) % 360.0f;
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.itemRenderState.isEmpty()) {
            return;
        }

        ps.pushPose();
        ps.translate(0.5, 0.46, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(state.rotation));
        ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
        ps.scale(0.75f, 0.75f, 0.75f);
        state.itemRenderState.submit(ps, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }
}
