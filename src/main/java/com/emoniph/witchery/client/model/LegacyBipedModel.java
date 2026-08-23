package com.emoniph.witchery.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Exact pose pipeline for Minecraft 1.7.10 {@code ModelBiped}. */
public class LegacyBipedModel<S extends LegacyBipedModel.State> extends HumanoidModel<S> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "legacy_biped"), "main");

    public LegacyBipedModel(ModelPart root) {
        super(root);
    }

    public static MeshDefinition createLegacyMesh() {
        return HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(createLegacyMesh(), 64, 32);
    }

    @Override
    public void setupAnim(S state) {
        float walk = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        float age = state.ageInTicks;

        head.x = 0.0F;
        head.y = state.sourceCrouching ? 1.0F : 0.0F;
        head.z = 0.0F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.zRot = 0.0F;

        body.x = 0.0F;
        body.y = 0.0F;
        body.z = 0.0F;
        body.xRot = state.sourceCrouching ? 0.5F : 0.0F;
        body.yRot = 0.0F;
        body.zRot = 0.0F;

        rightArm.x = -5.0F;
        rightArm.y = 2.0F;
        rightArm.z = 0.0F;
        rightArm.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * speed;
        rightArm.yRot = 0.0F;
        rightArm.zRot = 0.0F;
        leftArm.x = 5.0F;
        leftArm.y = 2.0F;
        leftArm.z = 0.0F;
        leftArm.xRot = Mth.cos(walk * 0.6662F) * speed;
        leftArm.yRot = 0.0F;
        leftArm.zRot = 0.0F;

        rightLeg.x = -1.9F;
        rightLeg.y = state.sourceCrouching ? 9.0F : 12.0F;
        rightLeg.z = state.sourceCrouching ? 4.0F : 0.1F;
        rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.4F * speed;
        rightLeg.yRot = 0.0F;
        rightLeg.zRot = 0.0F;
        leftLeg.x = 1.9F;
        leftLeg.y = state.sourceCrouching ? 9.0F : 12.0F;
        leftLeg.z = state.sourceCrouching ? 4.0F : 0.1F;
        leftLeg.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 1.4F * speed;
        leftLeg.yRot = 0.0F;
        leftLeg.zRot = 0.0F;

        if (state.sourceRiding) {
            rightArm.xRot -= Mth.PI / 5.0F;
            leftArm.xRot -= Mth.PI / 5.0F;
            rightLeg.xRot = -Mth.PI * 2.0F / 5.0F;
            leftLeg.xRot = -Mth.PI * 2.0F / 5.0F;
            rightLeg.yRot = Mth.PI / 10.0F;
            leftLeg.yRot = -Mth.PI / 10.0F;
        }
        if (state.heldItemLeft != 0) {
            leftArm.xRot = leftArm.xRot * 0.5F - Mth.PI / 10.0F * state.heldItemLeft;
        }
        if (state.heldItemRight != 0) {
            rightArm.xRot = rightArm.xRot * 0.5F - Mth.PI / 10.0F * state.heldItemRight;
        }

        applySourceSwing(state.swingProgress);
        if (state.sourceCrouching) {
            rightArm.xRot += 0.4F;
            leftArm.xRot += 0.4F;
        }
        addSourceArmIdle(age);
        if (state.aimedBow) applySourceBow(age);
        // Modern HumanoidModel nests hat below head; a zero local pose reproduces the
        // source's independent headwear part with the same pivot and rotation once.
        hat.setPos(0.0F, 0.0F, 0.0F);
        hat.setRotation(0.0F, 0.0F, 0.0F);
    }

    private void applySourceSwing(float progress) {
        if (progress <= -9990.0F) return;
        float bodyYaw = Mth.sin(Mth.sqrt(progress) * Mth.TWO_PI) * 0.2F;
        body.yRot = bodyYaw;
        rightArm.z = Mth.sin(bodyYaw) * 5.0F;
        rightArm.x = -Mth.cos(bodyYaw) * 5.0F;
        leftArm.z = -Mth.sin(bodyYaw) * 5.0F;
        leftArm.x = Mth.cos(bodyYaw) * 5.0F;
        rightArm.yRot += bodyYaw;
        leftArm.yRot += bodyYaw;
        leftArm.xRot += bodyYaw;
        float eased = 1.0F - progress;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float strike = Mth.sin(eased * Mth.PI);
        float headCompensation = Mth.sin(progress * Mth.PI) * -(head.xRot - 0.7F) * 0.75F;
        rightArm.xRot -= strike * 1.2F + headCompensation;
        rightArm.yRot += bodyYaw * 2.0F;
        rightArm.zRot = Mth.sin(progress * Mth.PI) * -0.4F;
    }

    private void addSourceArmIdle(float age) {
        float roll = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        float pitch = Mth.sin(age * 0.067F) * 0.05F;
        rightArm.zRot += roll;
        leftArm.zRot -= roll;
        rightArm.xRot += pitch;
        leftArm.xRot -= pitch;
    }

    private void applySourceBow(float age) {
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.yRot = -0.1F + head.yRot;
        leftArm.yRot = 0.5F + head.yRot;
        rightArm.xRot = -Mth.HALF_PI + head.xRot;
        leftArm.xRot = -Mth.HALF_PI + head.xRot;
        addSourceArmIdle(age);
    }

    public static class State extends HumanoidRenderState {
        public boolean sourceRiding;
        public boolean sourceCrouching;
        public int heldItemLeft;
        public int heldItemRight;
        public boolean aimedBow;
        public float swingProgress;
    }
}
