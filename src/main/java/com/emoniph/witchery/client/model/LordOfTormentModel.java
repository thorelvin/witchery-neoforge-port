package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.LordOfTormentEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Lord of Torment model — ported from ModelLordOfTorment (128×128). */
public class LordOfTormentModel extends EntityModel<LordOfTormentModel.LordOfTormentRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "lord_of_torment"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm1, rightArm2;
    private final ModelPart leftArm1,  leftArm2;
    private final ModelPart rightLeg,  leftLeg;
    private final ModelPart wingsLeft, wingsRight;

    public LordOfTormentModel(ModelPart root) {
        super(root);
        this.head      = root.getChild("head");
        this.body      = root.getChild("body");
        this.rightArm1 = root.getChild("right_arm1");
        this.rightArm2 = root.getChild("right_arm2");
        this.leftArm1  = root.getChild("left_arm1");
        this.leftArm2  = root.getChild("left_arm2");
        this.rightLeg  = root.getChild("right_leg");
        this.leftLeg   = root.getChild("left_leg");
        this.wingsLeft  = root.getChild("wings_left");
        this.wingsRight = root.getChild("wings_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head (0,0) — skull + beard tendrils + nose + horns
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                    .texOffs(0,  0).addBox(-4, -8, -4, 8, 8, 8)   // skull
                    .texOffs(34, 0).addBox(-3,  0, -4, 1, 7, 1)   // beard1
                    .texOffs(34, 0).addBox(-2,  0, -5, 1, 5, 1)   // beard2
                    .texOffs(34, 0).addBox(-1,  0, -4, 1, 9, 1)   // beard3
                    .texOffs(34, 0).addBox( 0,  0, -5, 1, 6, 1)   // beard4
                    .texOffs(34, 0).addBox( 1,  0, -4, 1, 4, 1)   // beard5
                    .texOffs(34, 0).addBox( 2,  0, -5, 1, 8, 1)   // beard6
                    .texOffs(40, 0).addBox(-3, -4, -5, 6, 4, 1)   // nose
                    .texOffs(40, 6).addBox(-2, -6, -5, 4, 2, 1),  // nose2
                PartPose.offset(0, -6, 0));

        // Horns as children of head
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(55, 0).addBox(-2, -15, 0, 1, 9, 1),
                PartPose.offsetAndRotation(0, 0, 0, 0.5948578f, 0, -0.1858931f));
        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(55, 0).addBox(1, -15, 0, 1, 9, 1),
                PartPose.offsetAndRotation(0, 0, 0, 0.5948578f, 0, 0.1858931f));

        // Body (16,16)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4, -6, -2, 8, 14, 4),
                PartPose.ZERO);

        // Arms (40,16)
        root.addOrReplaceChild("right_arm1",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 20, 4),
                PartPose.offset(-5, -4, 0));
        root.addOrReplaceChild("right_arm2",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 20, 4),
                PartPose.offset(-5, -4, 0));
        root.addOrReplaceChild("left_arm1",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1, -2, -2, 4, 20, 4),
                PartPose.offset(5, -4, 0));
        root.addOrReplaceChild("left_arm2",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1, -2, -2, 4, 20, 4),
                PartPose.offset(5, -4, 0));

        // Legs (0,16)
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 15, 4),
                PartPose.offset(-2, 8, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 15, 4),
                PartPose.offset(2, 8, 0));

        // Wings — zero-depth planes (0,42) and (0,82)
        root.addOrReplaceChild("wings_left",
                CubeListBuilder.create().texOffs(0, 42).addBox(-20, -20, 0, 20, 40, 0),
                PartPose.offset(0, 1, 5));
        root.addOrReplaceChild("wings_right",
                CubeListBuilder.create().texOffs(0, 82).addBox(0, -20, 0, 20, 40, 0),
                PartPose.offset(0, 1, 5));

        return LayerDefinition.create(mesh, 128, 128);
    }

        @Override
    public void setupAnim(LordOfTormentRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = netHeadYaw  * Mth.DEG_TO_RAD;
        head.xRot = headPitch   * Mth.DEG_TO_RAD;

        // Arm bob animation
        rightArm1.xRot = Mth.cos(3.8077927f) * 2.0f * limbSwingAmount * 0.5f;
        rightArm2.xRot = Mth.cos(3.8077927f) * 2.0f * limbSwingAmount * 0.25f;
        leftArm1.xRot  = Mth.cos(0.6662f)    * 2.0f * limbSwingAmount * 0.5f;
        leftArm2.xRot  = Mth.cos(0.6662f)    * 2.0f * limbSwingAmount * 0.25f;

        // Idle arm sway
        float swayC = Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f;
        float swayS = Mth.sin(ageInTicks * 0.067f) * 0.05f;
        rightArm1.zRot =  swayC;  rightArm1.xRot += swayS;
        rightArm2.zRot = -swayC;  rightArm2.xRot -= swayS;
        leftArm1.zRot  = -swayC;  leftArm1.xRot  -= swayS;
        leftArm2.zRot  =  swayC;  leftArm2.xRot  += swayS;

        // Wing flap — spread when moving, slow wave when idle
        if (state.moving) {
            wingsLeft.yRot  =  0.4f;
            wingsRight.yRot = -0.4f;
        } else {
            float wave = Mth.cos(ageInTicks * 0.09f) * 0.3f;
            wingsLeft.yRot  = Mth.cos(3.8077927f) * 2.0f * limbSwingAmount * 0.5f + wave;
            wingsRight.yRot = Mth.cos(3.8077927f) * 2.0f * limbSwingAmount * 0.5f - wave;
        }

        // Attack animation
        if (state.attackTimer > 0.0F) {
            float t = smoothstep(state.attackTimer, 15.0f);
            rightArm1.xRot = -1.5f + 0.8f * t;
            leftArm1.xRot  = -1.5f + 0.8f * t;
            rightArm1.zRot = -(- 1.5f + 1.5f * t);
            leftArm1.zRot  =   -1.5f + 1.5f * t;
            rightArm2.zRot = -(-1.0f + 1.5f * t);
            leftArm2.zRot  =   -1.0f + 1.5f * t;
        }
    }

    /** Mirrors original func_78172_a: triangle wave normalised to [-1, 1]. */
    private static float smoothstep(float timer, float period) {
        return (Math.abs(timer % period - period * 0.5f) - period * 0.25f) / (period * 0.25f);
    }

    public static class LordOfTormentRenderState extends LivingEntityRenderState {
        public float attackTimer;
        public boolean moving;
    }
}
