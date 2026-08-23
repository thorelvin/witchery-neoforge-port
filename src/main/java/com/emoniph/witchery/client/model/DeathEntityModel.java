package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DeathEntityModel extends EntityModel<DeathEntityModel.DeathRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.tryParse("witchery:death"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart robe;
    private final ModelPart scythe;

    public DeathEntityModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.robe = root.getChild("robe");
        this.scythe = rightArm.getChild("scythe");
        this.robe.xScale = 1.05F;
        this.robe.zScale = 1.05F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(27, 43).addBox(-4.0F, -8.0F, -4.0F, 8, 10, 8),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Body
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Left Arm (right arm defined later alongside scythe children)
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Right Leg
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2),
            PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Left Leg
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2),
            PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Robe
        root.addOrReplaceChild("robe",
            CubeListBuilder.create().texOffs(0, 33).addBox(-4.0F, 0.0F, -2.5F, 8, 23, 5),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Scythe (child of right arm, pivot at -6, 10, 0)
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        // scythe rotation matches original ModelDeath: setRotation(0,0,0) — no rotation
        rightArmDef.addOrReplaceChild("scythe",
            CubeListBuilder.create().mirror()
                    .texOffs(58, 5).addBox(-0.5F, -16.0F, -0.5F, 1, 35, 1)
                    .texOffs(36, 0).addBox(0.0F, -16.0F, 0.0F, 13, 4, 0),
            PartPose.offset(-6.0F, 10.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

        @Override
    public void setupAnim(DeathRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F - (float) (Math.PI / 2);
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.x = -5.0F;
        leftArm.x = 5.0F;
        rightArm.z = 0.0F;
        leftArm.z = 0.0F;

        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;

        // Original render: rotationPointX=-0.8, rotationPointY=8.1, rotationPointZ=0, rotateAngleX=PI/2
        scythe.x = -0.8F;
        scythe.y = 8.1F;
        scythe.z = 0.0F;
        scythe.yRot = 0.0F;
        scythe.zRot = 0.0F;
        scythe.xRot = (float) (Math.PI / 2);

        float attack = state.attackProgress;
        body.yRot = Mth.sin(Mth.sqrt(attack) * Mth.TWO_PI) * 0.2F;
        rightArm.z = Mth.sin(body.yRot) * 5.0F;
        rightArm.x = -Mth.cos(body.yRot) * 5.0F;
        leftArm.z = -Mth.sin(body.yRot) * 5.0F;
        leftArm.x = Mth.cos(body.yRot) * 5.0F;
        rightArm.yRot += body.yRot;
        leftArm.yRot += body.yRot;
        leftArm.xRot += body.yRot;
        float easedAttack = 1.0F - attack;
        easedAttack *= easedAttack;
        easedAttack *= easedAttack;
        easedAttack = 1.0F - easedAttack;
        float attackSwing = Mth.sin(easedAttack * Mth.PI);
        float headCompensation = Mth.sin(attack * Mth.PI) * -(head.xRot - 0.7F) * 0.75F;
        rightArm.xRot -= attackSwing * 1.2F + headCompensation;
        rightArm.yRot += body.yRot * 2.0F;
        rightArm.zRot = Mth.sin(attack * Mth.PI) * -0.4F;

        body.xRot = 0.0F;
        rightLeg.z = 0.1F;
        leftLeg.z = 0.1F;
        rightLeg.y = 12.0F;
        leftLeg.y = 12.0F;
        head.y = 0.0F;
        float armRoll = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        rightArm.zRot += armRoll;
        leftArm.zRot -= armRoll;
        float armPitch = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot += armPitch;
        leftArm.xRot -= armPitch;
        robe.xRot = 0.0F;
    }

    public static class DeathRenderState extends LivingEntityRenderState {
        public float attackProgress;
    }
}
