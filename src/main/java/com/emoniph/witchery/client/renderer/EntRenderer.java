package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.EntModel;
import com.emoniph.witchery.entity.EntEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class EntRenderer extends MobRenderer<EntEntity, EntModel.EntRenderState, EntModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/ent.png");

    public EntRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EntModel(ctx.bakeLayer(EntModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override public EntModel.EntRenderState createRenderState() { return new EntModel.EntRenderState(); }

    @Override
    public void extractRenderState(EntEntity entity, EntModel.EntRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        int attackTimer = entity.getAttackTimer();
        state.attackTimer = attackTimer > 0 ? Math.max(0.0f, attackTimer - partialTick) : 0.0f;
        state.screaming = entity.isScreaming();
    }

    @Override
    protected void setupRotations(EntModel.EntRenderState state, PoseStack poseStack,
                                  float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        float roll = sourceWalkingRollDegrees(state.walkAnimationPos, state.walkAnimationSpeed);
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    @Override
    protected void scale(EntModel.EntRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    static float sourceWalkingRollDegrees(float walkPosition, float walkSpeed) {
        if (walkSpeed < 0.01F) {
            return 0.0F;
        }
        float shifted = walkPosition + 6.0F;
        float triangle = (Math.abs(shifted % 13.0F - 6.5F) - 3.25F) / 3.25F;
        return 6.5F * triangle;
    }

    @Override public Identifier getTextureLocation(EntModel.EntRenderState state) { return TEXTURE; }
}
