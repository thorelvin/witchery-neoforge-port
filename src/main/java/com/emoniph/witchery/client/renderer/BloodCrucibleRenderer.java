package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.BloodCrucibleBlockEntity;
import com.emoniph.witchery.client.model.BloodCrucibleModel;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BloodCrucibleRenderer implements BlockEntityRenderer<BloodCrucibleBlockEntity, BloodCrucibleRenderer.State> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/block/blood_crucible.png");

    // White tint (1,1,1) — lets the crimson blood texture show through as-is.
    // Using a dark red tint previously multiplied the texture to near-black.
    private static final int BLOOD_COLOR = ARGB.colorFromFloat(0.88F, 1.0F, 1.0F, 1.0F);

    private final BloodCrucibleModel model;

    public static class State extends BlockEntityRenderState {
        public int bloodLevel = 0;
    }

    public BloodCrucibleRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new BloodCrucibleModel(ctx.bakeLayer(BloodCrucibleModel.LAYER_LOCATION));
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(BloodCrucibleBlockEntity be, State state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.bloodLevel = be.getBloodLevel();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
        ps.translate(0.0F, -1.0F, 0.0F);

        for (var part : model.bodyParts()) {
            collector.submitModelPart(part, ps, RenderTypes.entityCutoutNoCull(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }

        if (state.bloodLevel > 0) {
            ps.pushPose();
            ps.translate(0.0F, bloodYOffset(state.bloodLevel), 0.0F);
            collector.submitModelPart(model.blood(), ps, RenderTypes.entityTranslucent(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null, BLOOD_COLOR, null);
            ps.popPose();
        }

        ps.popPose();
    }

    /**
     * Positive offset in the 180°-flipped pose = moving DOWN in world Y.
     * Blood at model y=20 sits at block y=0.25. We shift it down for partial fills.
     *   level 5  → block y=0.0625 → offset +3/16
     *   level 10 → block y=0.125  → offset +2/16
     *   level 15 → block y=0.1875 → offset +1/16
     *   level 20 → block y=0.25   → offset 0
     */
    private static float bloodYOffset(int bloodLevel) {
        return switch (bloodLevel / 5) {
            case 1 -> 3.0F / 16.0F;
            case 2 -> 2.0F / 16.0F;
            case 3 -> 1.0F / 16.0F;
            default -> 0.0F;
        };
    }
}
