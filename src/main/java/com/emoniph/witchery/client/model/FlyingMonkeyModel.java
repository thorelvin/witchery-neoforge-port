package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
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

public class FlyingMonkeyModel extends EntityModel<FlyingMonkeyModel.FlyingMonkeyRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "flying_monkey"), "main");

    private static final float TAIL_BASE_X = -0.63739425F;
    private static final float BODY_BASE_X = 0.59184116F;
    private static final float ARM_BASE_X = -0.18203785F;
    private static final float WING_BASE_X = -0.68294734F;
    private static final float RIGHT_WING_BASE_Y = 0.3642502F;
    private static final float RIGHT_WING_BASE_Z = 0.5462881F;
    private static final float LEFT_WING_BASE_Y = -0.3642502F;
    private static final float LEFT_WING_BASE_Z = -0.5462881F;

    private final ModelPart tail;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart shoulder;
    private final ModelPart body;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public static class FlyingMonkeyRenderState extends LivingEntityRenderState {
        public boolean landed;
        public boolean sitting;
        public int tintColor = -1;
    }

    public FlyingMonkeyModel(ModelPart root) {
        super(root);
        this.tail = root.getChild("tail");
        this.leftArm = root.getChild("leftArm");
        this.rightArm = root.getChild("rightArm");
        this.shoulder = root.getChild("shoulder");
        this.body = root.getChild("body");
        this.leftLeg = root.getChild("leftLeg");
        this.rightLeg = root.getChild("rightLeg");
        this.head = root.getChild("head");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(18, 23).addBox(-0.5F, -7.8F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 18.5F, 5.3F, TAIL_BASE_X, 0.0F, 0.0F));
        root.addOrReplaceChild("rightArm",
                CubeListBuilder.create().texOffs(0, 19).addBox(-2.0F, -1.1F, -1.0F, 2.0F, 11.0F, 2.0F),
                PartPose.offsetAndRotation(-3.5F, 14.0F, 0.0F, ARM_BASE_X, 0.0F, 0.0F));
        root.addOrReplaceChild("leftArm",
                CubeListBuilder.create().texOffs(0, 19).mirror().addBox(-0.5F, -1.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 14.0F, 0.0F, ARM_BASE_X, 0.0F, 0.0F));
        root.addOrReplaceChild("rightLeg",
                CubeListBuilder.create().texOffs(9, 25).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(-1.4F, 19.0F, 4.7F));
        root.addOrReplaceChild("leftLeg",
                CubeListBuilder.create().texOffs(9, 25).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(1.4F, 19.0F, 4.7F));
        root.addOrReplaceChild("shoulder",
                CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, -3.0F, -3.0F, 7.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 1.0F, Mth.HALF_PI, 0.0F, 0.0F));
        root.addOrReplaceChild("rightWing",
                CubeListBuilder.create().texOffs(28, 25).addBox(-12.0F, -0.5F, -3.0F, 12.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(-1.0F, 14.0F, 2.5F, WING_BASE_X, RIGHT_WING_BASE_Y, RIGHT_WING_BASE_Z));
        root.addOrReplaceChild("leftWing",
                CubeListBuilder.create().texOffs(28, 25).mirror().addBox(0.0F, -0.5F, -3.0F, 12.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(1.0F, 14.0F, 2.5F, WING_BASE_X, LEFT_WING_BASE_Y, LEFT_WING_BASE_Z));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(36, 12).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 15.8F, 2.0F, BODY_BASE_X, 0.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 12.0F, -1.5F));
        head.addOrReplaceChild("headEarRight",
                CubeListBuilder.create().texOffs(18, 14).addBox(-4.5F, -2.5F, -1.5F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("headEarLeft",
                CubeListBuilder.create().texOffs(18, 14).mirror().addBox(2.5F, -2.5F, -1.5F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("headFace",
                CubeListBuilder.create().texOffs(5, 12).addBox(-2.5F, -3.5F, -3.5F, 5.0F, 5.0F, 1.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("headNose",
                CubeListBuilder.create().texOffs(9, 19).addBox(-2.0F, -1.5F, -4.5F, 4.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(FlyingMonkeyRenderState state) {
        super.setupAnim(state);
        float ageInTicks = state.ageInTicks;
        float limbSwingAmount = state.walkAnimationSpeed;

        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.zRot = 0.0F;

        if (state.landed) {
            setLandedPose();
        } else {
            setFlightPose(ageInTicks);
        }

        if (state.sitting) {
            setSittingPose(state.landed, ageInTicks);
        } else {
            setStandingPose();
        }

        animateTail(ageInTicks, limbSwingAmount);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
    }

    private void setLandedPose() {
        this.leftWing.setRotation(WING_BASE_X, LEFT_WING_BASE_Y, LEFT_WING_BASE_Z);
        this.rightWing.setRotation(WING_BASE_X, RIGHT_WING_BASE_Y, RIGHT_WING_BASE_Z);
        this.leftArm.xRot = ARM_BASE_X;
        this.rightArm.xRot = ARM_BASE_X;
        this.leftLeg.xRot = 0.0F;
        this.rightLeg.xRot = 0.0F;
        this.leftWing.y = 14.0F;
        this.rightWing.y = 14.0F;
    }

    private void setFlightPose(float ageInTicks) {
        float wingZ = Mth.cos(ageInTicks * 0.5F) * Mth.PI * 0.4F + 0.2F;
        this.rightWing.zRot = wingZ;
        this.rightWing.xRot = 0.0F;
        this.rightWing.y = 12.0F;
        this.leftWing.zRot = -wingZ;
        this.leftWing.xRot = 0.0F;
        this.leftWing.y = 12.0F;
        this.leftArm.xRot = 0.2F;
        this.rightArm.xRot = 0.2F;
        this.leftLeg.xRot = 0.1F;
        this.rightLeg.xRot = 0.1F;
        wiggleArms(ageInTicks);
        this.rightLeg.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftLeg.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }

    private void setSittingPose(boolean landed, float ageInTicks) {
        this.rightLeg.xRot = -1.3F;
        this.rightLeg.z = 2.0F;
        this.rightLeg.y = 21.0F;
        this.leftLeg.xRot = -1.3F;
        this.leftLeg.z = 2.0F;
        this.leftLeg.y = 21.0F;
        this.body.xRot = 0.1F;
        this.body.y = 17.0F;
        this.tail.setPos(0.0F, 20.0F, 4.0F);
        this.tail.xRot = -0.9F;
        wiggleArms(ageInTicks);
        if (!landed) {
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
        }
    }

    private void setStandingPose() {
        this.body.setPos(0.0F, 15.8F, 2.0F);
        this.body.xRot = BODY_BASE_X;
        this.rightLeg.setPos(-1.4F, 19.0F, 4.7F);
        this.leftLeg.setPos(1.4F, 19.0F, 4.7F);
        this.rightLeg.xRot = 0.0F;
        this.leftLeg.xRot = 0.0F;
        this.tail.setPos(0.0F, 18.5F, 5.3F);
        this.tail.xRot = TAIL_BASE_X;
    }

    private void wiggleArms(float ageInTicks) {
        float zWiggle = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.zRot += zWiggle;
        this.leftArm.zRot -= zWiggle;
        this.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }

    private void animateTail(float ageInTicks, float limbSwingAmount) {
        if (limbSwingAmount > 0.1F) {
            this.tail.xRot += -limbSwingAmount - 0.1F;
            this.tail.zRot += 5.0F * Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        } else {
            this.tail.zRot += 3.0F * Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        }
    }
}
