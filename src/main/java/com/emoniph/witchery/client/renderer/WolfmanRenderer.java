package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WolfmanModel;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public class WolfmanRenderer extends MobRenderer<WolfmanEntity, WolfmanRenderer.WolfmanRenderState, WolfmanModel> {
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/wolfman_entity.png");

    public WolfmanRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfmanModel(context.bakeLayer(WolfmanModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public WolfmanRenderState createRenderState() {
        return new WolfmanRenderState();
    }

    @Override
    public void extractRenderState(WolfmanEntity entity, WolfmanRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        int attackTimer = entity.getAttackTimer();
        state.attackTimer = attackTimer > 0 ? Math.max(0.0F, attackTimer - partialTick) : 0.0F;
        state.attackProgress = entity.getAttackAnim(partialTick);
        state.sourceSitting = false;
        state.sourceCrouching = entity.isCrouching();
        populateHeldItemState(entity.getMainHandItem(), entity.isUsingItem(), state);
    }

    static void populateHeldItemState(ItemStack heldItem, boolean usingItem, WolfmanRenderState state) {
        state.heldItemLeft = 0;
        state.heldItemRight = heldItem.isEmpty() ? 0 : 1;
        state.aimedBow = false;
        if (!heldItem.isEmpty() && usingItem) {
            ItemUseAnimation animation = heldItem.getUseAnimation();
            if (animation == ItemUseAnimation.BLOCK) {
                state.heldItemRight = 3;
            } else if (animation == ItemUseAnimation.BOW) {
                state.aimedBow = true;
            }
        }
    }

    static void copyAnimationState(LivingEntityRenderState source, WolfmanRenderState target) {
        target.walkAnimationPos = source.walkAnimationPos;
        target.walkAnimationSpeed = source.walkAnimationSpeed;
        target.ageInTicks = source.ageInTicks;
        target.yRot = source.yRot;
        target.xRot = source.xRot;
        target.partialTick = source.partialTick;
    }

    @Override
    protected void scale(WolfmanRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F,
                LEGACY_BASE_DELTA_Y - (state.sourceCrouching ? 0.125F : 0.0F), 0.0F);
    }

    @Override
    public Identifier getTextureLocation(WolfmanRenderState state) {
        return TEXTURE;
    }

    public static class WolfmanRenderState extends LivingEntityRenderState {
        public float attackTimer;
        public float attackProgress;
        public boolean sourceSitting;
        public boolean sourceCrouching;
        public int heldItemLeft;
        public int heldItemRight;
        public boolean aimedBow;
    }
}
