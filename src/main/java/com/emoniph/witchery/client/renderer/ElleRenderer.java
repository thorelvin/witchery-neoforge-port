package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LegacyBipedModel;
import com.emoniph.witchery.entity.ElleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

/** Source follower-type-zero renderer used by Elle. */
public class ElleRenderer extends HumanoidMobRenderer<ElleEntity, ElleRenderer.State,
        LegacyBipedModel<ElleRenderer.State>> {
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/elle.png");

    public ElleRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyBipedModel<>(context.bakeLayer(LegacyBipedModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
        ArmorModelSet<HumanoidModel<State>> armor =
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElleEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sourceRiding = entity.isPassenger();
        state.sourceCrouching = entity.isCrouching();
        state.swingProgress = entity.getAttackAnim(partialTick);
        populateHeldItemState(entity.getMainHandItem(), entity.isUsingItem(), state);
    }

    private static void populateHeldItemState(ItemStack heldItem, boolean usingItem, State state) {
        state.heldItemLeft = 0;
        state.heldItemRight = heldItem.isEmpty() ? 0 : 1;
        state.aimedBow = false;
        if (!heldItem.isEmpty() && usingItem) {
            ItemUseAnimation animation = heldItem.getUseAnimation();
            if (animation == ItemUseAnimation.BLOCK) state.heldItemRight = 3;
            else if (animation == ItemUseAnimation.BOW) state.aimedBow = true;
        }
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.translate(0.0F,
                LEGACY_BASE_DELTA_Y - (state.sourceCrouching ? 0.125F : 0.0F), 0.0F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    public static class State extends LegacyBipedModel.State {
    }
}
