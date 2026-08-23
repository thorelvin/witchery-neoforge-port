package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.DemonEntityModel;
import com.emoniph.witchery.entity.DemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class DemonEntityRenderer extends MobRenderer<DemonEntity, DemonEntityModel.DemonRenderState, DemonEntityModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/demon.png");

    public DemonEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DemonEntityModel(ctx.bakeLayer(DemonEntityModel.LAYER_LOCATION)),
                DemonEntity.SOURCE_SHADOW_RADIUS);
    }

    @Override
    public DemonEntityModel.DemonRenderState createRenderState() {
        return new DemonEntityModel.DemonRenderState();
    }

    @Override
    public void extractRenderState(DemonEntity entity, DemonEntityModel.DemonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        int attackTimer = entity.getAttackTimer();
        state.attackTimer = attackTimer > 0 ? Math.max(0.0F, attackTimer - partialTick) : 0.0F;
        state.lightCoords = LightTexture.FULL_BRIGHT;
    }

    @Override
    public Identifier getTextureLocation(DemonEntityModel.DemonRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(DemonEntityModel.DemonRenderState state, PoseStack poseStack,
                                  float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        applySourceWalkingRoll(poseStack, state.walkAnimationPos, state.walkAnimationSpeed);
    }

    static void applySourceWalkingRoll(PoseStack poseStack, float walkPosition, float walkSpeed) {
        if (walkSpeed >= 0.01F) {
            float roll = 6.5F * DemonEntityModel.triangleWave(walkPosition + 6.0F, 13.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }
}
