package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.BrazierBlockEntity;
import com.emoniph.witchery.client.model.BrazierModel;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BrazierRenderer implements BlockEntityRenderer<BrazierBlockEntity, BrazierRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/brazier.png");
    private final BrazierModel model;

    public static class State extends BlockEntityRenderState {
        public int ingredientCount;
        public int contentsColor;
        public boolean burning;
    }

    public BrazierRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new BrazierModel(ctx.bakeLayer(BrazierModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(BrazierBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.ingredientCount = be.getIngredientCount();
        state.contentsColor = be.getContentsColor();
        state.burning = be.isBurning();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, -1, 0);
        if (state.ingredientCount > 0) {
            model.setAshIngredientCount(state.ingredientCount);
            int light = state.burning ? LightTexture.FULL_BRIGHT : state.lightCoords;
            collector.submitModelPart(model.ashPart(), ps, RenderTypes.entityTranslucent(TEXTURE),
                    light, OverlayTexture.NO_OVERLAY, null, state.contentsColor, null);
        }
        ps.popPose();
    }
}
