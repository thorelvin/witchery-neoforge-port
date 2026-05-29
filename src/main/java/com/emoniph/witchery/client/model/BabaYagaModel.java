package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Baba Yaga model — ported from ModelBabaYaga (1.7.10-0.24.1).
 * 128×128 texture matching babayaga.png.
 *
 * Villager-proportion body (8×10×8 head, shorter arms 4×8×4, 4×10×4 legs)
 * plus a 3-tier stacked witch hat with brim, mortar prop, and cloak.
 * UV positions are pixel coordinates within the 128×128 texture.
 */
public class BabaYagaModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "baba_yaga"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public BabaYagaModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Head — villager 8×10×8 + nose + 3-tier witch hat ─────────────────
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4, -10, -4, 8, 10, 8)   // head
                        .texOffs(24, 0).addBox(-1, -3, -6, 2, 4, 2),   // villager nose
                PartPose.offset(0, 0, 0));

        // Hat brim — wide flat plate at the hat base
        PartDefinition hatBase = head.addOrReplaceChild("hat_brim",
                CubeListBuilder.create().texOffs(0, 98).addBox(0, 0, 0, 14, 2, 14),
                PartPose.offset(-7, -12, -7));

        // Hat tier 1 (middle section)
        PartDefinition hat1 = hatBase.addOrReplaceChild("hat_tier1",
                CubeListBuilder.create().texOffs(0, 76).addBox(0, 0, 0, 7, 4, 7),
                PartPose.offsetAndRotation(3.75f, -4, 4, -0.05235988f, 0, 0.02617994f));

        // Hat tier 2 (upper section)
        PartDefinition hat2 = hat1.addOrReplaceChild("hat_tier2",
                CubeListBuilder.create().texOffs(0, 87).addBox(0, 0, 0, 4, 4, 4),
                PartPose.offsetAndRotation(1.75f, -4, 2, -0.10471976f, 0, 0.05235988f));

        // Hat tip (narrow top)
        hat2.addOrReplaceChild("hat_tip",
                CubeListBuilder.create().texOffs(0, 95)
                        .addBox(0, 0, 0, 1, 2, 1, new CubeDeformation(0.25f)),
                PartPose.offsetAndRotation(1.75f, -2, 2, (float)(-Math.PI / 15), 0, 0.10471976f));

        // ── Body — villager 8×12×6 (slightly wider/deeper than standard) ──────
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 20).addBox(-4, 0, -3, 8, 12, 6),
                PartPose.offset(0, 0, 0));

        // ── Arms — shorter villager arms 4×8×4 ───────────────────────────────
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(44, 22).addBox(-3, -2, -2, 4, 8, 4),
                PartPose.offset(-5, 2, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-1, -2, -2, 4, 8, 4),
                PartPose.offset(5, 2, 0));

        // ── Legs — villager 4×10×4 ────────────────────────────────────────────
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2, 0, -2, 4, 10, 4),
                PartPose.offset(-2, 12, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2, 0, -2, 4, 10, 4),
                PartPose.offset(2, 12, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing  = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float age        = state.ageInTicks;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Arm swing
        rightArm.xRot = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 2.0f * limbAmount * 0.5f;
        leftArm.xRot  = Mth.cos(limbSwing * 0.6662f)           * 2.0f * limbAmount * 0.5f;
        rightArm.zRot = 0;
        leftArm.zRot  = 0;

        // Leg swing
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f)           * 1.4f * limbAmount;
        leftLeg.xRot  = Mth.cos(limbSwing * 0.6662f + Mth.PI)  * 1.4f * limbAmount;

        // Idle sway on arms (witch style)
        float sway = Mth.sin(age * 0.067f) * 0.05f;
        rightArm.xRot += sway;
        leftArm.xRot  -= sway;

        // Slight body lean forward (villager posture)
        body.xRot = 0.1f;
    }
}
