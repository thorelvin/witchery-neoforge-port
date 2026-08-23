package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LegacyBipedModel;
import com.emoniph.witchery.client.model.WitchHunterModel;
import com.emoniph.witchery.entity.WitchHunterEntity;
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

public class WitchHunterRenderer extends HumanoidMobRenderer<WitchHunterEntity,
        WitchHunterRenderer.WitchHunterRenderState, WitchHunterModel> {
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter3.png")
    };

    public WitchHunterRenderer(EntityRendererProvider.Context context) {
        super(context, new WitchHunterModel(context.bakeLayer(WitchHunterModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
        ArmorModelSet<HumanoidModel<WitchHunterRenderState>> armor =
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public WitchHunterRenderState createRenderState() {
        return new WitchHunterRenderState();
    }

    @Override
    public void extractRenderState(WitchHunterEntity entity, WitchHunterRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.hunterType = entity.getHunterType();
        state.sourceRiding = entity.isPassenger();
        state.sourceCrouching = entity.isCrouching();
        state.swingProgress = entity.getAttackAnim(partialTick);
        populateHeldItemState(entity.getMainHandItem(), entity.isUsingItem(), state);
    }

    static void populateHeldItemState(ItemStack heldItem, boolean usingItem,
                                      WitchHunterRenderState state) {
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

    @Override
    protected void scale(WitchHunterRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F,
                LEGACY_BASE_DELTA_Y - (state.sourceCrouching ? 0.125F : 0.0F), 0.0F);
    }

    @Override
    public Identifier getTextureLocation(WitchHunterRenderState state) {
        return SKINS[Math.floorMod(state.hunterType, SKINS.length)];
    }

    public static class WitchHunterRenderState extends LegacyBipedModel.State {
        public int hunterType;
    }
}
