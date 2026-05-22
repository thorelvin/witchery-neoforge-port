package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Poltergeist — ghostly biped with duplicate arm pairs for ripple effect. 64×32 texture.
 * Ported 1:1 from original ModelPoltergeist (witchery-1.7.2-0.19.0).
 * Distinct from GhostModel/Spectre — poltergeist.png uses this UV layout.
 */
public class PoltergeistModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "poltergeist"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart rightArm2;
    private final ModelPart leftArm;
    private final ModelPart leftArm2;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PoltergeistModel(ModelPart root) {
        super(root);
        this.head      = root.getChild("head");
        this.body      = root.getChild("body");
        this.rightArm  = root.getChild("right_arm");
        this.rightArm2 = root.getChild("right_arm_2");
        this.leftArm   = root.getChild("left_arm");
        this.leftArm2  = root.getChild("left_arm_2");
        this.rightLeg  = root.getChild("right_leg");
        this.leftLeg   = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-4, -8, -3, 8, 8, 6),
                PartPose.offset(0, 0, 0));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(16, 16).addBox(-4, 0, -1, 8, 11, 2),
                PartPose.offset(0, 0, 0));

        // Duplicate arm pairs — both at same position, animated at half/full amplitude
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(40, 0).addBox(-1, -2, -1, 2, 18, 2),
                PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("right_arm_2",
                CubeListBuilder.create().mirror().texOffs(40, 0).addBox(-1, -2, -1, 2, 18, 2),
                PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1, -2, -1, 2, 18, 2),
                PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("left_arm_2",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1, -2, -1, 2, 18, 2),
                PartPose.offset(5, 2, 0));

        // Legs — bottom at y=11+13=24 (floor reference)
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-1, 0, -1, 2, 13, 2),
                PartPose.offset(-2, 11, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1, 0, -1, 2, 13, 2),
                PartPose.offset(2, 11, 0));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing       = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks      = state.ageInTicks;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Primary arms at full amplitude, secondary at half — gives a ripple/echo effect
        float armSwing = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 2.0f * limbSwingAmount * 0.5f;
        rightArm.xRot  =  armSwing;
        rightArm2.xRot =  armSwing * 0.5f;
        leftArm.xRot   = -armSwing;
        leftArm2.xRot  = -armSwing * 0.5f;

        // Ghostly float sway on z-axis
        float sway = Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f;
        rightArm.zRot  =  sway;
        rightArm2.zRot = -sway;
        leftArm.zRot   = -sway;
        leftArm2.zRot  =  sway;

        float legSwing = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.xRot =  legSwing;
        leftLeg.xRot  = -legSwing;
    }
}
