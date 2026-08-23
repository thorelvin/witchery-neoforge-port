package com.emoniph.witchery.client.model;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Exact legacy pig graph and familiar poses from Witchery 0.24.1. */
public final class SpectralFamiliarModel
        extends QuadrupedModel<SpectralFamiliarModel.State> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "spectral_familiar"), "main");

    public static final class State extends LivingEntityRenderState {
        public boolean sitting;
        public boolean tamed;
        public boolean searching;
    }

    public SpectralFamiliarModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8)
                        .texOffs(16, 16).addBox(-2.0F, 0.0F, -9.0F, 4, 3, 1),
                PartPose.offset(0.0F, 12.0F, -6.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(28, 8)
                        .addBox(-5.0F, -10.0F, -7.0F, 10, 16, 8),
                PartPose.offsetAndRotation(0.0F, 11.0F, 2.0F, Mth.HALF_PI, 0.0F, 0.0F));
        addLeg(root, "right_hind_leg", -3.0F, 7.0F);
        addLeg(root, "left_hind_leg", 3.0F, 7.0F);
        addLeg(root, "right_front_leg", -3.0F, -5.0F);
        addLeg(root, "left_front_leg", 3.0F, -5.0F);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(State state) {
        resetPose();
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        if (state.sitting) {
            body.xRot = Mth.PI / 4.0F;
            body.y = 14.5F;
            rightHindLeg.xRot = leftHindLeg.xRot = -Mth.PI / 20.0F;
            rightHindLeg.y = leftHindLeg.y = 15.8F;
            rightHindLeg.z = leftHindLeg.z = -7.0F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -Mth.HALF_PI;
            rightFrontLeg.y = leftFrontLeg.y = 21.0F;
            rightFrontLeg.z = leftFrontLeg.z = 1.0F;
            return;
        }

        float walk = state.walkAnimationPos * 0.6662F;
        float speed = state.walkAnimationSpeed;
        body.xRot = Mth.HALF_PI;
        rightHindLeg.xRot = Mth.cos(walk + Mth.PI) * speed;
        leftHindLeg.xRot = Mth.cos(walk) * speed;
        rightFrontLeg.xRot = Mth.cos(walk) * speed;
        leftFrontLeg.xRot = Mth.cos(walk + Mth.PI) * speed;
    }

    public ModelPart sourceHead() {
        return head;
    }

    public ModelPart sourceBody() {
        return body;
    }

    public ModelPart sourceRightHindLeg() {
        return rightHindLeg;
    }

    public ModelPart sourceLeftHindLeg() {
        return leftHindLeg;
    }

    public ModelPart sourceRightFrontLeg() {
        return rightFrontLeg;
    }

    public ModelPart sourceLeftFrontLeg() {
        return leftFrontLeg;
    }

    private static void addLeg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(x, 18.0F, z));
    }
}
