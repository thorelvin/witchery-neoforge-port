package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Flame Imp model — ported from ModelImp (64×64). */
public class FlameImpModel extends EntityModel<FlameImpModel.FlameImpRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "flame_imp"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm, leftArm;
    private final ModelPart rightLeg, leftLeg;
    private final ModelPart chest;

    public static class FlameImpRenderState extends LivingEntityRenderState {
        public float attackProgress;
        public boolean powered;
        public boolean riding;
    }

    public FlameImpModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
        this.chest    = root.getChild("chest");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head (0,0) 10×8×10, pivot (0,8,0)
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5, -8, -4, 10, 8, 10),
                PartPose.offset(0, 8, 0));
        // Horns as children of head
        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(0, 21).addBox(-1, -5, -1, 2, 5, 2),
                PartPose.offsetAndRotation(4, -5, 0, 0.4089647f, 0, 0.7435722f));
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(0, 21).addBox(-1, -5, -1, 2, 5, 2),
                PartPose.offsetAndRotation(-4, -5, 0, 0.4089647f, 0, -0.7435722f));
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(9, 21).addBox(-1, 0, -1, 2, 4, 2),
                PartPose.offsetAndRotation(0, -4, -3, -0.9666439f, 0, 0));

        // Body (0,48) 8×9×7, pivot (0,9,0)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 48).addBox(-4, 0, -4, 8, 9, 7),
                PartPose.offset(0, 9, 0));

        // Chest (4,41) 6×2×4, pivot (1,8,0)
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(4, 41).addBox(-4, 0, -2, 6, 2, 4),
                PartPose.offset(1, 8, 0));

        // Arms (41,0), pivot (-5,11,0) and (5,11,0)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(41, 0).addBox(-2, -2, -1.5f, 3, 13, 3),
                PartPose.offset(-5, 11, 0));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(41, 0).addBox(-1, -2, -1.5f, 3, 13, 3),
                PartPose.offset(5, 11, 0));

        // Legs (33,48), pivot (-1.5,18,0) and (1.5,18,0)
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(33, 48).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
                PartPose.offset(-1.5f, 18, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(33, 48).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
                PartPose.offset(1.5f, 18, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FlameImpRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = netHeadYaw  * Mth.DEG_TO_RAD;
        head.xRot = headPitch   * Mth.DEG_TO_RAD;

        // Arm swing
        rightArm.xRot = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 2.0f * limbSwingAmount * 0.5f;
        leftArm.xRot  = Mth.cos(limbSwing * 0.6662f)           * 2.0f * limbSwingAmount * 0.5f;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.x = -5.0F;
        leftArm.x = 5.0F;
        rightArm.z = 0.0F;
        leftArm.z = 0.0F;

        // Leg swing
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f)           * 1.4f * limbSwingAmount;
        leftLeg.xRot  = Mth.cos(limbSwing * 0.6662f + Mth.PI)  * 1.4f * limbSwingAmount;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;

        if (state.riding) {
            rightArm.xRot -= Mth.PI / 5.0F;
            leftArm.xRot -= Mth.PI / 5.0F;
            rightLeg.xRot = -Mth.TWO_PI / 5.0F;
            leftLeg.xRot = -Mth.TWO_PI / 5.0F;
            rightLeg.yRot = Mth.PI / 10.0F;
            leftLeg.yRot = -Mth.PI / 10.0F;
        }

        applyAttackSwing(state.attackProgress);

        body.xRot = 0.0F;
        rightLeg.z = 0.1F;
        leftLeg.z = 0.1F;

        // Idle arm sway
        float swayC = Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f;
        float swayS = Mth.sin(ageInTicks * 0.067f) * 0.05f;
        rightArm.zRot += swayC;  rightArm.xRot += swayS;
        leftArm.zRot  -= swayC;  leftArm.xRot  -= swayS;

    }

    private void applyAttackSwing(float swingProgress) {
        body.yRot = Mth.sin(Mth.sqrt(swingProgress) * Mth.PI * 2.0F) * 0.2F;
        rightArm.z = Mth.sin(body.yRot) * 5.0F;
        rightArm.x = -Mth.cos(body.yRot) * 5.0F;
        leftArm.z = -Mth.sin(body.yRot) * 5.0F;
        leftArm.x = Mth.cos(body.yRot) * 5.0F;
        rightArm.yRot += body.yRot;
        leftArm.yRot += body.yRot;
        leftArm.xRot += body.yRot;

        float eased = 1.0F - swingProgress;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float punch = Mth.sin(eased * Mth.PI);
        float headDip = Mth.sin(swingProgress * Mth.PI) * -(head.xRot - 0.7F) * 0.75F;
        rightArm.xRot -= punch * 1.2F + headDip;
        rightArm.yRot += body.yRot * 2.0F;
        rightArm.zRot = Mth.sin(swingProgress * Mth.PI) * -0.4F;
    }
}
