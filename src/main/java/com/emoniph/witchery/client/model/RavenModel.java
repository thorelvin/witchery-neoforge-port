package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RavenModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "raven"), "main");

    private static final float HEAD_BASE_PITCH = 0.0873F;
    private static final float TAIL_BASE_PITCH = -0.3927F;
    private static final float WING_BASE_RIGHT = 0.1745F;
    private static final float WING_BASE_LEFT = -0.1745F;

    private final ModelPart head;
    private final ModelPart upperBeak;
    private final ModelPart bottomBeak;
    private final ModelPart tail;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart wingRight;
    private final ModelPart wingRight2;
    private final ModelPart wingRight3;
    private final ModelPart wingLeft;
    private final ModelPart wingLeft2;
    private final ModelPart wingLeft3;

    public RavenModel(ModelPart root) {
        super(root);
        ModelPart rootz = root.getChild("rootz");
        this.head = rootz.getChild("head");
        this.upperBeak = this.head.getChild("upperBeak");
        this.bottomBeak = this.head.getChild("bottomBeak");
        ModelPart body = rootz.getChild("body");
        this.tail = body.getChild("tail");
        this.rightLeg = body.getChild("rightLeg");
        this.leftLeg = body.getChild("leftLeg");
        this.wingRight = body.getChild("wingRight");
        this.wingRight2 = this.wingRight.getChild("wingRight2");
        this.wingRight3 = this.wingRight2.getChild("wingRight3");
        this.wingLeft = body.getChild("wingLeft");
        this.wingLeft2 = this.wingLeft.getChild("wingLeft2");
        this.wingLeft3 = this.wingLeft2.getChild("wingLeft3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rootz = root.addOrReplaceChild("rootz", CubeListBuilder.create(), PartPose.offset(0.0F, 21.25F, 0.0F));
        PartDefinition head = rootz.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 27).addBox(-2.5F, -8.0F, -2.0F, 5.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(36, 41).addBox(-2.5F, -7.0F, 2.0F, 5.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -5.25F, -0.5F, HEAD_BASE_PITCH, 0.0F, 0.0F));
        head.addOrReplaceChild("upperBeak",
                CubeListBuilder.create()
                        .texOffs(13, 44).addBox(-1.0F, -1.9457F, -4.288F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 51).addBox(-1.0F, -2.0957F, -2.288F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.15F)),
                PartPose.offset(0.0F, -4.7543F, -1.462F));
        head.addOrReplaceChild("bottomBeak",
                CubeListBuilder.create()
                        .texOffs(44, 13).addBox(-1.0F, 0.2991F, -4.2411F, 2.0F, 1.0F, 4.0F, new CubeDeformation(-0.15F))
                        .texOffs(52, 4).addBox(-0.5F, 0.4491F, -1.8911F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, -5.1491F, -1.5089F));
        PartDefinition rightEye = head.addOrReplaceChild("rightEye",
                CubeListBuilder.create().texOffs(25, 54).addBox(-0.45F, -0.9925F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.01F)),
                PartPose.offset(-2.07F, -6.0175F, -1.02F));
        rightEye.addOrReplaceChild("rightEyeball",
                CubeListBuilder.create()
                        .texOffs(26, 44).addBox(-1.0F, -0.775F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.6F))
                        .texOffs(26, 49).addBox(-1.0F, -1.225F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.6F)),
                PartPose.offset(-0.05F, -0.0075F, -0.25F));
        PartDefinition leftEye = head.addOrReplaceChild("leftEye",
                CubeListBuilder.create().texOffs(55, 19).addBox(-0.55F, -0.9925F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.01F)),
                PartPose.offset(2.07F, -6.0175F, -1.02F));
        leftEye.addOrReplaceChild("leftEyeball",
                CubeListBuilder.create()
                        .texOffs(49, 31).addBox(-1.0F, -0.775F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.6F))
                        .texOffs(49, 36).addBox(-1.0F, -1.225F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.6F)),
                PartPose.offset(0.05F, -0.0075F, -0.25F));

        PartDefinition body = rootz.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -6.5978F, -1.5088F));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.ZERO);
        chest.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(33, 0).addBox(-3.5F, -3.0F, 7.0F, 7.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(33, 8).addBox(-3.5F, -3.0F, 9.0F, 7.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-3.5F, -3.0F, -2.0F, 7.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.5978F, 1.5088F, -0.5672F, 0.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 0).addBox(-1.5F, -1.0F, 8.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.9978F, 10.5088F, TAIL_BASE_PITCH, 0.0F, 0.0F));
        PartDefinition rightLeg = body.addOrReplaceChild("rightLeg",
                CubeListBuilder.create().texOffs(44, 19).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.01F)),
                PartPose.offsetAndRotation(-1.75F, 5.3478F, 3.5088F, 0.0F, 0.2182F, 0.0F));
        rightLeg.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(32, 55).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, -0.2618F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("rightLegFinger4",
                CubeListBuilder.create().texOffs(37, 55).addBox(-6.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(6.0F, 3.6F, -0.45F));
        PartDefinition rightLegFinger1 = rightLeg.addOrReplaceChild("rightLegFinger1", CubeListBuilder.create(), PartPose.offset(5.1589F, 3.65F, -0.8907F));
        rightLegFinger1.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(49, 41).addBox(-0.2836F, -0.5F, -2.4763F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.2182F, 0.0F));
        PartDefinition rightLegFinger3 = rightLeg.addOrReplaceChild("rightLegFinger3", CubeListBuilder.create(), PartPose.offset(6.8411F, 3.65F, -0.8907F));
        rightLegFinger3.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(49, 46).addBox(-0.7164F, -0.5F, -2.4763F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, -0.2182F, 0.0F));
        rightLeg.addOrReplaceChild("rightLegFinger2",
                CubeListBuilder.create().texOffs(50, 8).addBox(-6.5F, -0.5F, -2.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(6.0F, 3.65F, -0.95F));

        PartDefinition leftLeg = body.addOrReplaceChild("leftLeg",
                CubeListBuilder.create().texOffs(49, 25).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.01F)),
                PartPose.offsetAndRotation(1.75F, 5.3478F, 3.5088F, 0.0F, -0.2182F, 0.0F));
        leftLeg.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(44, 55).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.0257F, 1.0F, 0.0084F, -0.2618F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("leftLegFinger4",
                CubeListBuilder.create().texOffs(0, 56).addBox(-6.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(5.9743F, 3.6F, -0.4416F));
        PartDefinition leftLegFinger1 = leftLeg.addOrReplaceChild("leftLegFinger1", CubeListBuilder.create(), PartPose.offset(5.1332F, 3.65F, -0.8823F));
        leftLegFinger1.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(35, 50).addBox(-0.2836F, -0.5F, -2.4763F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.2182F, 0.0F));
        PartDefinition leftLegFinger3 = leftLeg.addOrReplaceChild("leftLegFinger3", CubeListBuilder.create(), PartPose.offset(6.8154F, 3.65F, -0.8823F));
        leftLegFinger3.addOrReplaceChild("cube_r7",
                CubeListBuilder.create().texOffs(0, 51).addBox(-0.7164F, -0.5F, -2.4763F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, -0.2182F, 0.0F));
        leftLeg.addOrReplaceChild("leftLegFinger2",
                CubeListBuilder.create().texOffs(9, 51).addBox(-6.5F, -0.5F, -2.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(5.9743F, 3.65F, -0.9416F));

        PartDefinition wingRight = body.addOrReplaceChild("wingRight",
                CubeListBuilder.create().texOffs(27, 16).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.5978F, 2.7588F, -0.4363F, 0.1309F, WING_BASE_RIGHT));
        PartDefinition wingRight2 = wingRight.addOrReplaceChild("wingRight2",
                CubeListBuilder.create().texOffs(36, 30).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -3.0F, 6.0F));
        wingRight2.addOrReplaceChild("wingRight3",
                CubeListBuilder.create().texOffs(18, 51).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 5.0F));

        PartDefinition wingLeft = body.addOrReplaceChild("wingLeft",
                CubeListBuilder.create().texOffs(19, 30).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.5978F, 2.7588F, -0.4363F, -0.1309F, WING_BASE_LEFT));
        PartDefinition wingLeft2 = wingLeft.addOrReplaceChild("wingLeft2",
                CubeListBuilder.create().texOffs(0, 40).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -3.0F, 6.0F));
        wingLeft2.addOrReplaceChild("wingLeft3",
                CubeListBuilder.create().texOffs(53, 51).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = HEAD_BASE_PITCH + state.xRot * Mth.DEG_TO_RAD * 0.65F;

        float wingFlap = Mth.sin(ageInTicks * 1.2F) * (0.18F + Mth.clamp(limbSwingAmount * 0.55F, 0.0F, 0.55F));
        this.wingRight.zRot = WING_BASE_RIGHT + wingFlap;
        this.wingLeft.zRot = WING_BASE_LEFT - wingFlap;
        this.wingRight2.zRot = wingFlap * 0.7F;
        this.wingLeft2.zRot = -wingFlap * 0.7F;
        this.wingRight3.zRot = wingFlap * 0.45F;
        this.wingLeft3.zRot = -wingFlap * 0.45F;

        this.rightLeg.xRot = Mth.cos(limbSwing * 0.9F) * 1.1F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.9F + Mth.PI) * 1.1F * limbSwingAmount;
        this.tail.xRot = TAIL_BASE_PITCH + limbSwingAmount * 0.15F + Mth.sin(ageInTicks * 0.08F) * 0.03F;

        this.upperBeak.xRot = 0.0F;
        this.bottomBeak.xRot = 0.0F;
    }
}
