package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ToadModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "toad"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart armRight;
    private final ModelPart armLeft;
    private final ModelPart legRight;
    private final ModelPart legLeft;

    public ToadModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.armRight = root.getChild("arm_right");
        this.armLeft  = root.getChild("arm_left");
        this.legRight = root.getChild("leg_right");
        this.legLeft  = root.getChild("leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head: nose + two eyes as sub-boxes
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 5).addBox(-2, -1, -4, 4, 2, 4)          // nose
                        .texOffs(0, 0).addBox(-2, -3, -3, 2, 2, 2)           // eyeRight (float offsets rounded)
                        .texOffs(8, 0).addBox(0, -3, -3, 2, 2, 2),           // eyeLeft
                PartPose.offset(0, 20, -1));

        // Body slightly tilted
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2, -1, 0, 4, 2, 5),
                PartPose.offsetAndRotation(0, 20, -1, -0.4833F, 0, 0));

        // Front arms
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(13, 26).addBox(-1, 0, 0, 1, 4, 1),
                PartPose.offsetAndRotation(-2, 20, -1, -0.3346F, 0, 0));

        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(18, 26).addBox(0, 0, 0, 1, 4, 1),
                PartPose.offsetAndRotation(2, 20, -1, -0.3346F, 0, 0));

        // Hind legs with thigh + flat foot
        PartDefinition legR = root.addOrReplaceChild("leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-2, -1, -2, 2, 2, 3)          // thigh
                        .texOffs(0, 26).addBox(-3, 1, -4, 3, 0, 3),          // foot (flat)
                PartPose.offset(-2, 23, 3));

        PartDefinition legL = root.addOrReplaceChild("leg_left",
                CubeListBuilder.create()
                        .texOffs(11, 20).addBox(0, -1, -2, 2, 2, 3)          // thigh
                        .texOffs(0, 26).addBox(0, 1, -4, 3, 0, 3),           // foot (flat)
                PartPose.offset(2, 23, 3));

        return LayerDefinition.create(mesh, 32, 32);
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

        // Leg animation while walking
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        legRight.xRot = swing;
        legLeft.xRot  = swing;
        armLeft.xRot  = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount + (-0.3346F);
        armRight.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount + (-0.3346F);
    }
}
