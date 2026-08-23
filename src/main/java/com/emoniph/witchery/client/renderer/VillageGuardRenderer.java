package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LegacyBipedModel;
import com.emoniph.witchery.client.model.VillageGuardModel;
import com.emoniph.witchery.entity.VillageGuardEntity;
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

public class VillageGuardRenderer extends HumanoidMobRenderer<VillageGuardEntity,
        VillageGuardRenderer.State, VillageGuardModel<VillageGuardRenderer.State>> {
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/village_guard1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/village_guard2.png")
    };

    public VillageGuardRenderer(EntityRendererProvider.Context context) {
        super(context,
                new VillageGuardModel<>(context.bakeLayer(VillageGuardModel.LAYER_LOCATION), true),
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
    public void extractRenderState(VillageGuardEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.guardType = entity.getGuardType();
        state.sourceRiding = entity.isPassenger();
        state.sourceCrouching = entity.isCrouching();
        state.swingProgress = entity.getAttackAnim(partialTick);
        populateHeldItemState(entity.getMainHandItem(), entity.isUsingItem(), state);
    }

    static void populateHeldItemState(ItemStack heldItem, boolean usingItem, State state) {
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
        return SKINS[Math.floorMod(state.guardType, SKINS.length)];
    }

    public static class State extends LegacyBipedModel.State {
        public int guardType;
    }
}
