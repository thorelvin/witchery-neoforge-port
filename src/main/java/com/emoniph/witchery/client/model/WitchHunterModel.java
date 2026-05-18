package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.WitchHunterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Standard biped with a wide-brimmed hat and long coat overlay.
 * Ported from original ModelWitchHunter (64×64 texture).
 */
public class WitchHunterModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "witch_hunter"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public WitchHunterModel(ModelPart root) {
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

        // Standard biped parts (matches witchhunter skin UV layout)
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5f)),
                PartPose.offset(0, 0, 0));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4),
                PartPose.offset(-5, 2, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4),
                PartPose.offset(5, 2, 0));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-1.9f, 12, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(1.9f, 12, 0));

        // Hunter hat — wide brim + tall top (children of head)
        head.addOrReplaceChild("hat_brim",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-7, 0, -7, 14, 1, 14),
                PartPose.offset(0, -6, 0));

        head.addOrReplaceChild("hat_top",
                CubeListBuilder.create().texOffs(33, 48).mirror().addBox(-3, 0, -3, 6, 2, 6),
                PartPose.offset(0, -10, 0));

        // Long coat overlay (child of body)
        body.addOrReplaceChild("coat_bottom",
                CubeListBuilder.create().texOffs(1, 47).mirror().addBox(-5, 0, -2.5f, 10, 11, 5),
                PartPose.offset(0, 0, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

        @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        rightLeg.xRot =  swing;
        leftLeg.xRot  = -swing;
        rightArm.xRot = -swing;
        leftArm.xRot  =  swing;

        // Raise bow arm toward target when attacking
        if (false) { // entity.isAggressive() removed
            rightArm.xRot = -1.2F;
        }
    }
}
