package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HornedHuntsmanModel;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class HornedHuntsmanRenderer extends MobRenderer<HornedHuntsmanEntity,
        HornedHuntsmanModel.HuntsmanRenderState, HornedHuntsmanModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final int SOURCE_LIGHT = LightTexture.FULL_BRIGHT;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/avatar.png");

    public HornedHuntsmanRenderer(EntityRendererProvider.Context context) {
        super(context, new HornedHuntsmanModel(context.bakeLayer(HornedHuntsmanModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public HornedHuntsmanModel.HuntsmanRenderState createRenderState() {
        return new HornedHuntsmanModel.HuntsmanRenderState();
    }

    @Override
    public void extractRenderState(HornedHuntsmanEntity entity,
                                   HornedHuntsmanModel.HuntsmanRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTimer = entity.getAttackTimer();
        state.lightCoords = SOURCE_LIGHT;
    }

    @Override
    protected void setupRotations(HornedHuntsmanModel.HuntsmanRenderState state,
                                  PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        float roll = sourceWalkingRoll(state.walkAnimationPos, state.walkAnimationSpeed);
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    @Override
    protected void scale(HornedHuntsmanModel.HuntsmanRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(HornedHuntsmanModel.HuntsmanRenderState state) {
        return TEXTURE;
    }

    static float sourceWalkingRoll(float walkAnimationPos, float walkAnimationSpeed) {
        return walkAnimationSpeed >= 0.01F
                ? 5.5F * HornedHuntsmanModel.triangleWave(walkAnimationPos + 6.0F, 13.0F)
                : 0.0F;
    }
}
