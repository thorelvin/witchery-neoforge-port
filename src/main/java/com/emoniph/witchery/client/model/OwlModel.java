package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class OwlModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "owl"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public OwlModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head with sub-parts: main skull, beak, two horns
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4, -6, -3, 8, 6, 6)
                        .texOffs(30, 0).addBox(-1, -3, -4, 2, 3, 1)
                        .texOffs(37, 0).addBox(-5, -7, -1, 1, 3, 2)
                        .texOffs(37, 0).addBox(4, -7, -1, 1, 3, 2),
                PartPose.offset(0, 15, 0));

        // Body with legs as children
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-3, 0, -2, 6, 8, 4),
                PartPose.offset(0, 15, 0));

        body.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -2, 2, 1, 4),
                PartPose.offset(-2, 8, -1));

        body.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -2, 2, 1, 4),
                PartPose.offset(2, 8, -1));

        // Wings (arms)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1, -1, -2, 1, 8, 4),
                PartPose.offset(-3, 16, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(0, -1, -2, 1, 8, 4),
                PartPose.offset(3, 16, 0));

        return LayerDefinition.create(mesh, 64, 32);
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

        // Wing flap animation
        float flapSpeed = ageInTicks * 0.5F;
        rightArm.zRot = Mth.cos(flapSpeed) * Mth.PI * 0.2F * 2.0F + 1.4F;
        leftArm.zRot  = -rightArm.zRot;
    }
}
