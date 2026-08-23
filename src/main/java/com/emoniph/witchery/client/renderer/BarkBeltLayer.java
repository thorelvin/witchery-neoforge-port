package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BarkGrowthModel;
import com.emoniph.witchery.item.BarkBeltItem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumSet;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

/** Restores the source charge-grown Bark Belt geometry around the wearer's animated biped pivots. */
public final class BarkBeltLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>>
        extends RenderLayer<S, M> {
    static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(
            "witchery", "textures/entity/equipment/humanoid_leggings/bark_belt_base.png");
    static final Identifier OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath(
            "witchery", "textures/entity/equipment/humanoid_leggings/bark_belt_overlay.png");

    private final BarkGrowthModel growth;

    public BarkBeltLayer(RenderLayerParent<S, M> parent, EntityRendererProvider.Context context) {
        super(parent);
        this.growth = new BarkGrowthModel(
                context.bakeLayer(BarkGrowthModel.TALL_UV_LAYER),
                context.bakeLayer(BarkGrowthModel.WIDE_UV_LAYER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       S state, float limbSwing, float limbSwingAmount) {
        if (!state.legsEquipment.is(com.emoniph.witchery.init.WitcheryItems.BARK_BELT.get())) return;

        int charge = Math.min(BarkBeltItem.getChargeLevel(state.legsEquipment),
                BarkBeltItem.getMaxChargeLevel(
                        state.headEquipment, state.chestEquipment, state.legsEquipment, state.feetEquipment));
        EnumSet<BarkGrowthModel.Group> groups = groupsFor(charge,
                BarkBeltItem.supportsBarkGrowth(state.feetEquipment),
                BarkBeltItem.supportsBarkGrowth(state.chestEquipment),
                BarkBeltItem.supportsBarkGrowth(state.headEquipment));
        if (groups.isEmpty()) return;

        submitPass(poseStack, collector, light, state, groups,
                RenderTypes.armorCutoutNoCull(BASE_TEXTURE));
        submitPass(poseStack, collector, light, state, groups,
                RenderTypes.armorCutoutNoCull(OVERLAY_TEXTURE));
        if (state.legsEquipment.hasFoil()) {
            submitPass(poseStack, collector, light, state, groups, RenderTypes.armorEntityGlint());
        }
    }

    static EnumSet<BarkGrowthModel.Group> groupsFor(int charge,
                                                    boolean shoes, boolean robes, boolean hat) {
        EnumSet<BarkGrowthModel.Group> groups = EnumSet.noneOf(BarkGrowthModel.Group.class);
        int remaining = charge;
        remaining = addCharged(groups, BarkGrowthModel.Group.BODY, remaining);
        remaining = addCharged(groups, BarkGrowthModel.Group.SPIKES, remaining);
        if (shoes) {
            remaining = addCharged(groups, BarkGrowthModel.Group.RIGHT_LEG, remaining);
            remaining = addCharged(groups, BarkGrowthModel.Group.LEFT_LEG, remaining);
        }
        if (robes) {
            remaining = addCharged(groups, BarkGrowthModel.Group.RIGHT_ARM, remaining);
            remaining = addCharged(groups, BarkGrowthModel.Group.LEFT_ARM, remaining);
        }
        if (hat) {
            remaining = addCharged(groups, BarkGrowthModel.Group.RIGHT_HEAD_SHOULDER, remaining);
            addCharged(groups, BarkGrowthModel.Group.LEFT_HEAD_SHOULDER, remaining);
        }
        return groups;
    }

    private static int addCharged(EnumSet<BarkGrowthModel.Group> groups,
                                  BarkGrowthModel.Group group, int remaining) {
        if (remaining >= 1) groups.add(group);
        return remaining - 1;
    }

    private void submitPass(PoseStack poseStack, SubmitNodeCollector collector, int light,
                            S state, EnumSet<BarkGrowthModel.Group> groups, RenderType renderType) {
        for (BarkGrowthModel.Group group : groups) {
            ModelPart anchor = anchorFor(group);
            boolean leg = group == BarkGrowthModel.Group.RIGHT_LEG || group == BarkGrowthModel.Group.LEFT_LEG;
            for (ModelPart part : growth.parts(group)) {
                poseStack.pushPose();
                applyLegacyAnchor(poseStack, anchor, leg, state.isCrouching);
                collector.submitModelPart(part, poseStack, renderType,
                        light, OverlayTexture.NO_OVERLAY, null, -1, null);
                poseStack.popPose();
            }
        }
    }

    private ModelPart anchorFor(BarkGrowthModel.Group group) {
        return switch (group) {
            case BODY, SPIKES -> getParentModel().body;
            case RIGHT_LEG -> getParentModel().rightLeg;
            case LEFT_LEG -> getParentModel().leftLeg;
            case RIGHT_ARM, RIGHT_HEAD_SHOULDER -> getParentModel().rightArm;
            case LEFT_ARM, LEFT_HEAD_SHOULDER -> getParentModel().leftArm;
        };
    }

    static void applyLegacyAnchor(PoseStack poseStack, ModelPart bodyPart,
                                  boolean leg, boolean crouching) {
        float pivotX = bodyPart.x / 16.0F;
        float pivotY = bodyPart.y / 16.0F;
        float pivotZ = bodyPart.z / 16.0F;
        boolean rotated = bodyPart.xRot != 0.0F || bodyPart.yRot != 0.0F || bodyPart.zRot != 0.0F;
        if (rotated || leg) {
            poseStack.translate(pivotX, pivotY, pivotZ);
            if (rotated) {
                poseStack.mulPose(new Quaternionf().rotationZYX(
                        bodyPart.zRot, bodyPart.yRot, bodyPart.xRot));
            }
            poseStack.translate(-pivotX, -pivotY, -pivotZ);
            if (crouching && leg) {
                poseStack.translate(0.0F, -3.0F / 16.0F, 4.0F / 16.0F);
            }
        } else if (bodyPart.x != 0.0F || bodyPart.y != 0.0F || bodyPart.z != 0.0F) {
            poseStack.translate(pivotX, pivotY, pivotZ);
        }
    }
}
