package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.EntEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Ent — large tree-spirit biped. 64×64 texture. */
public class EntModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "ent"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public EntModel(ModelPart root) {
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

        // Large head with bark face
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5, -8, -5, 10, 10, 10),
                PartPose.offset(0, -4, 0));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 20).addBox(-6, 0, -4, 12, 14, 8),
                PartPose.offset(0, -4, 0));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 0).addBox(-3, -1, -2, 5, 16, 5),
                PartPose.offsetAndRotation(-9, -3, 0, 0, 0, 0.2f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-2, -1, -2, 5, 16, 5),
                PartPose.offsetAndRotation(9, -3, 0, 0, 0, -0.2f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 42).addBox(-3, 0, -3, 6, 12, 6),
                PartPose.offset(-4, 10, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 42).mirror().addBox(-3, 0, -3, 6, 12, 6),
                PartPose.offset(4, 10, 0));

        return LayerDefinition.create(mesh, 256, 256);
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
        float swing = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.xRot =  swing;
        leftLeg.xRot  = -swing;
        rightArm.xRot = -swing * 0.5f;
        leftArm.xRot  =  swing * 0.5f;
    }
}
