package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Ported from original Witchery's ModelWerewolf (64×64 texture).
 * Hierarchy: root → body → neck → head (ears/snout/mane/fluffs)
 *                        → back
 *                        → right_arm / left_arm
 *                        → hip → tail
 *                              → right_leg → right_foot
 *                              → left_leg  → left_foot
 */
public class WerewolfModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "wolfman"), "main");
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart hip;
    private final ModelPart tail;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public WerewolfModel(ModelPart root) {
        super(root);
        this.body     = root.getChild("body");
        this.neck     = body.getChild("neck");
        this.head     = neck.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm  = body.getChild("left_arm");
        this.hip      = body.getChild("hip");
        this.tail     = hip.getChild("tail");
        this.rightLeg = hip.getChild("right_leg");
        this.leftLeg  = hip.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Body — hunched forward PI/9 (~20°)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 12).addBox(-5, 0, -2, 10, 7, 6),
                PartPose.offsetAndRotation(0, -2, -4, (float)(Math.PI / 9), 0, 0));

        body.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(0, 37).addBox(-5, 7, -2, 10, 3, 6),
                PartPose.ZERO);

        // Neck — invisible pivot that levels the head against body tilt
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0, 0, 0, (float)(-Math.PI / 9), 0, 0));

        // Head (child of neck)
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -4, -4.5f, 7, 6, 6),
                PartPose.offset(0, 0, -1));

        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(30, 0).addBox(-3, -6, -1.5f, 2, 2, 1),
                PartPose.ZERO);
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(30, 0).addBox(1, -6, -1.5f, 2, 2, 1),
                PartPose.ZERO);
        head.addOrReplaceChild("right_fluff",
                CubeListBuilder.create().texOffs(36, 0).addBox(-6.5f, -2, -3, 3, 4, 1),
                PartPose.ZERO);
        head.addOrReplaceChild("left_fluff",
                CubeListBuilder.create().texOffs(36, 0).mirror().addBox(3.5f, -2, -3, 3, 4, 1),
                PartPose.ZERO);
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(20, 0).addBox(-1.5f, -1, -7.5f, 3, 3, 3),
                PartPose.ZERO);
        head.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(26, 30).addBox(-3.5f, -4, 1.5f, 7, 6, 3),
                PartPose.ZERO);
        head.addOrReplaceChild("jaw_fluff",
                CubeListBuilder.create().texOffs(36, 0).addBox(-3.5f, 2, -4.5f, 7, 2, 6),
                PartPose.ZERO);

        // Arms — forward-tilted, tilted back to compensate body lean
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 12).mirror().addBox(-1, -1, -2, 4, 14, 4),
                PartPose.offsetAndRotation(5, 2, 1, (float)(-Math.PI / 9), 0, 0));
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 12).addBox(-3, -1, -2, 4, 14, 4),
                PartPose.offsetAndRotation(-5, 2, 1, (float)(-Math.PI / 9), 0, 0));

        // Hip — lower body pivot, offset down from body
        PartDefinition hip = body.addOrReplaceChild("hip",
                CubeListBuilder.create().texOffs(0, 25).addBox(-4, 0, -2, 8, 7, 5),
                PartPose.offset(0, 7, 0));

        hip.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(48, 8).addBox(-1.5f, 0, -1.5f, 3, 8, 3),
                PartPose.offset(0, 5.5f, 2));

        // Digitigrade legs — bent at initial pose
        PartDefinition rightLeg = hip.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 46).addBox(-2, 0, -2, 4, 9, 4),
                PartPose.offsetAndRotation(-2.5f, 0, 0, -0.68294734f, 0, 0));
        rightLeg.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(16, 46).addBox(-2, 5, 2, 4, 9, 4),
                PartPose.ZERO);

        PartDefinition leftLeg = hip.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-2, 0, -2, 4, 9, 4),
                PartPose.offsetAndRotation(2.5f, 0, 0, -0.68294734f, 0, 0));
        leftLeg.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(16, 46).mirror().addBox(-2, 5, 2, 4, 9, 4),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        rightLeg.xRot = swing  - 0.68294734f;
        leftLeg.xRot  = -swing - 0.68294734f;
        rightArm.xRot = -swing - (float)(Math.PI / 9);
        leftArm.xRot  = swing  - (float)(Math.PI / 9);
        float walkTime = limbSwingAmount > 0.1F ? limbSwing : ageInTicks / 3f;
        tail.xRot = limbSwingAmount;
        tail.yRot = Mth.cos(walkTime * 0.3F) * (limbSwingAmount * 0.5F - 0.15F) + 0.05F;
    }
}
