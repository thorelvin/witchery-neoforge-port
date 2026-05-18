package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.FlameImpEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Flame Imp model — ported from ModelImp (64×64). */
public class FlameImpModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "flame_imp"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm, leftArm;
    private final ModelPart rightLeg, leftLeg;
    private final ModelPart chest;
    private final ModelPart wingRight, wingLeft;

    public FlameImpModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
        this.chest    = root.getChild("chest");
        this.wingRight = root.getChild("wing_right");
        this.wingLeft  = root.getChild("wing_left");
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

        // Wings (23,21) zero-depth planes, pivot (-2,10,-1) and (2,10,-1)
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(23, 21).addBox(0, 0, 0, 14, 21, 0),
                PartPose.offsetAndRotation(-2, 10, -1, 0.3047198f, -0.6698132f, -0.6283185f));
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(23, 21).addBox(0, 0, 0, 14, 21, 0),
                PartPose.offsetAndRotation(2, 10, -1, -0.3047198f, 3.8114066f, 0.6283185f));

        return LayerDefinition.create(mesh, 64, 64);
    }

        @Override
    public void setupAnim(LivingEntityRenderState state) {
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
        rightArm.zRot = 0; leftArm.zRot = 0;

        // Leg swing
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f)           * 1.4f * limbSwingAmount;
        leftLeg.xRot  = Mth.cos(limbSwing * 0.6662f + Mth.PI)  * 1.4f * limbSwingAmount;
        rightLeg.yRot = 0; leftLeg.yRot = 0;

        // Idle arm sway
        float swayC = Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f;
        float swayS = Mth.sin(ageInTicks * 0.067f) * 0.05f;
        rightArm.zRot += swayC;  rightArm.xRot += swayS;
        leftArm.zRot  -= swayC;  leftArm.xRot  -= swayS;

        // Leg spacing
        rightLeg.x = -1.5f + 0.1f;
        leftLeg.x  =  1.5f - 0.1f;
    }
}
