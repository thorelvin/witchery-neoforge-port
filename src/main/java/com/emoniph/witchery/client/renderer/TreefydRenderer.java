package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.TreefydModel;
import com.emoniph.witchery.entity.TreefydEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class TreefydRenderer extends MobRenderer<TreefydEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, TreefydModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/treefyd.png");
    public TreefydRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TreefydModel(ctx.bakeLayer(TreefydModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }
    @Override public net.minecraft.client.renderer.entity.state.LivingEntityRenderState createRenderState() { return new net.minecraft.client.renderer.entity.state.LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }

    @Override
    protected void setupRotations(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                  PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        float roll = sourceWalkingRoll(state.walkAnimationPos, state.walkAnimationSpeed);
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    @Override
    protected void scale(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                         PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    static float sourceWalkingRoll(float walkAnimationPos, float walkAnimationSpeed) {
        return walkAnimationSpeed >= 0.01F
                ? 5.0F * triangleWave(walkAnimationPos + 6.0F, 13.0F)
                : 0.0F;
    }

    private static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F)
                / (period * 0.25F);
    }
}
