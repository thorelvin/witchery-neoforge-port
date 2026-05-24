package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import com.emoniph.witchery.blocks.GrassperBlock;
import com.emoniph.witchery.client.model.GrassperModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class GrassperRenderer implements BlockEntityRenderer<GrassperBlockEntity, GrassperRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/grassper.png");

    private final ItemModelResolver itemModelResolver;
    private final GrassperModel model;

    public static class State extends BlockEntityRenderState {
        public ItemStackRenderState itemRenderState = new ItemStackRenderState();
        public float rotation = 0f;
        public float blockRotation = 0f;
        public boolean spinHeldItem = false;
    }

    public GrassperRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
        this.model = new GrassperModel(ctx.bakeLayer(GrassperModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GrassperBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        ItemStack stack = be.getItem();
        net.minecraft.world.level.Level beLevel = be.getLevel();
        if (!stack.isEmpty()) {
            Vec3 bePos = Vec3.atCenterOf(be.getBlockPos());
            itemModelResolver.updateForTopItem(state.itemRenderState, stack, ItemDisplayContext.GROUND, beLevel,
                    new net.minecraft.world.entity.ItemOwner() {
                        public net.minecraft.world.level.Level level() { return beLevel; }
                        public Vec3 position() { return bePos; }
                        public float getVisualRotationYInDegrees() { return 0f; }
                    }, 0);
        } else {
            state.itemRenderState = new ItemStackRenderState();
        }
        GraphicsPreset graphicsPreset = Minecraft.getInstance().options.graphicsPreset().get();
        state.spinHeldItem = graphicsPreset == GraphicsPreset.FANCY || graphicsPreset == GraphicsPreset.FABULOUS;
        state.rotation = state.spinHeldItem && beLevel != null
                ? ((beLevel.getGameTime() + partialTick) * 2.5f) % 360.0f
                : 0.0f;
        Direction facing = be.getBlockState().hasProperty(GrassperBlock.FACING)
                ? be.getBlockState().getValue(GrassperBlock.FACING)
                : Direction.NORTH;
        state.blockRotation = facing.getOpposite().toYRot();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0.0, -1.0, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(state.blockRotation));
        collector.submitModelPart(model.root(), ps, RenderTypes.entityCutout(TEXTURE), state.lightCoords,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null);
        ps.popPose();

        if (!state.itemRenderState.isEmpty()) {
            ps.pushPose();
            ps.translate(0.0f, 0.6f, 0.0f);
            ps.scale(0.75f, 0.75f, 0.75f);
            ps.translate(0.65f, -0.15f, 0.65f);
            if (state.spinHeldItem) {
                ps.mulPose(Axis.YP.rotationDegrees(state.rotation));
            }
            state.itemRenderState.submit(ps, collector, state.lightCoords,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
    }
}
