package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

class CrazyCatLadyHeldCatLayer extends RenderLayer<CrazyCatLadyRenderer.CrazyCatLadyRenderState, WitchModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    private final CatFamiliarModel model;
    private final CatFamiliarModel.CatFamiliarRenderState catState = new CatFamiliarModel.CatFamiliarRenderState();

    CrazyCatLadyHeldCatLayer(CrazyCatLadyRenderer parent,
                             net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) {
        super(parent);
        this.model = new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       CrazyCatLadyRenderer.CrazyCatLadyRenderState state, float yRot, float xRot) {
        if (!state.catReady || state.isInvisible) {
            return;
        }

        catState.sitting = false;
        catState.walkAnimationPos = 0.0F;
        catState.walkAnimationSpeed = 0.0F;
        catState.yRot = 0.0F;
        catState.xRot = 0.0F;
        model.setupAnim(catState);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.65F, -0.34F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.36F, 0.36F, 0.36F);
        poseStack.translate(0.0F, -1.15F, 0.0F);
        collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), null);
        poseStack.popPose();
    }
}
