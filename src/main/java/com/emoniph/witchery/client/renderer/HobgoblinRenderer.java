package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.HobgoblinModel;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class HobgoblinRenderer extends HumanoidMobRenderer<HobgoblinEntity,
        HobgoblinModel.HobgoblinRenderState, HobgoblinModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier[] TEXTURES_BY_PROFESSION = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin4.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin3.png"),
    };

    public HobgoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HobgoblinModel(ctx.bakeLayer(HobgoblinModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override public HobgoblinModel.HobgoblinRenderState createRenderState() {
        return new HobgoblinModel.HobgoblinRenderState();
    }

    @Override
    public void extractRenderState(HobgoblinEntity entity, HobgoblinModel.HobgoblinRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.profession = entity.getProfessionVariant();
        state.sourceCrouching = entity.isCrouching();
        state.worshipping = entity.isWorshipping();
        state.working = entity.isWorking();
        state.workingWithKoboldite = entity.getMainHandItem().is(WitcheryItems.KOBOLDITE_PICKAXE.get());
        state.holdingRightItem = !entity.getMainHandItem().isEmpty();
        state.holdingLeftItem = false;
        state.aimedBow = false;
        state.swingProgress = entity.getAttackAnim(partialTick);
        state.entityTick = entity.tickCount;
    }

    @Override
    protected void scale(HobgoblinModel.HobgoblinRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F,
                LEGACY_BASE_DELTA_Y - (state.sourceCrouching ? 0.125F : 0.0F), 0.0F);
    }

    @Override
    public Identifier getTextureLocation(HobgoblinModel.HobgoblinRenderState state) {
        return TEXTURES_BY_PROFESSION[Math.floorMod(state.profession, TEXTURES_BY_PROFESSION.length)];
    }
}
