package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.MindrakeModel;
import com.emoniph.witchery.entity.MindrakeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class MindrakeRenderer extends MobRenderer<MindrakeEntity, LivingEntityRenderState, MindrakeModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.25F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mindrake.png");

    public MindrakeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MindrakeModel(ctx.bakeLayer(MindrakeModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack,
                                  float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        float roll = sourceWalkingRoll(state.walkAnimationPos, state.walkAnimationSpeed);
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    static float sourceWalkingRoll(float walkAnimationPos, float walkAnimationSpeed) {
        return walkAnimationSpeed >= 0.01F
                ? 6.5F * triangleWave(walkAnimationPos + 6.0F, 13.0F)
                : 0.0F;
    }

    private static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F)
                / (period * 0.25F);
    }
}
