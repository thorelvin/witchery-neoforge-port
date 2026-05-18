package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.emoniph.witchery.entity.DeathEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DeathEntityModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.tryParse("witchery:death"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart robe;
    private final ModelPart scythe;
    private final ModelPart shaft;
    private final ModelPart blade;

    public DeathEntityModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.robe = root.getChild("robe");
        this.scythe = rightArm.getChild("scythe");
        this.shaft = scythe.getChild("shaft");
        this.blade = scythe.getChild("blade");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(27, 43).addBox(-4.0F, -8.0F, -4.0F, 8, 10, 8),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Body
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Left Arm (right arm defined later alongside scythe children)
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Right Leg
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2),
            PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Left Leg
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2),
            PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Robe
        root.addOrReplaceChild("robe",
            CubeListBuilder.create().texOffs(0, 33).addBox(-4.0F, 0.0F, -2.5F, 8, 23, 5),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Scythe (child of right arm, pivot at -6, 10, 0)
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition scytheDef = rightArmDef.addOrReplaceChild("scythe",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(-6.0F, 10.0F, 0.0F, (float)(Math.PI / 2), 0.0F, 0.0F));

        // Scythe Shaft
        scytheDef.addOrReplaceChild("shaft",
            CubeListBuilder.create().texOffs(58, 5).addBox(-0.5F, -16.0F, -0.5F, 1, 35, 1),
            PartPose.ZERO);

        // Scythe Blade
        scytheDef.addOrReplaceChild("blade",
            CubeListBuilder.create().texOffs(36, 0).addBox(0.0F, -16.0F, 0.0F, 13, 4, 0),
            PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

        @Override
    public void setupAnim(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        body.xRot = 0.0F;

        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F - (float) (Math.PI / 2);
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;

        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        scythe.yRot = -0.8F;
        scythe.zRot = 0.0F;
        scythe.xRot = (float) (Math.PI / 2);

        robe.xRot = 0.0F;
    }
}
