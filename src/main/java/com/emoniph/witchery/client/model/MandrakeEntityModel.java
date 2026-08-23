package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Mandrake mob - tiny screaming root creature.
 * Ported from original ModelMandrake (witchery-1.7.10-0.24.1).
 */
public class MandrakeEntityModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "mandrake_mob"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MandrakeEntityModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0, 16, 0));
        head.addOrReplaceChild("face",
                CubeListBuilder.create().mirror().texOffs(0, 8).addBox(-2, -4, -2, 4, 4, 4),
                PartPose.ZERO);
        head.addOrReplaceChild("leaves",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-4, -12, 0, 8, 8, 0),
                PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create(),
                PartPose.offset(0, 16, 0));
        body.addOrReplaceChild("bodyChest",
                CubeListBuilder.create().mirror().texOffs(21, 0).addBox(-2.5f, 0, -2.5f, 5, 2, 5),
                PartPose.ZERO);
        body.addOrReplaceChild("bodyBelly",
                CubeListBuilder.create().mirror().texOffs(17, 7).addBox(-3.5f, 2, -3.5f, 7, 3, 7),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(37, 0).addBox(-1, 0, -0.5f, 1, 3, 1),
                PartPose.offsetAndRotation(-2, 17, 0, 0, 0, 1.047198F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(37, 0).addBox(0, 0, -0.5f, 1, 3, 1),
                PartPose.offsetAndRotation(2, 17, 0, 0, 0, -1.047198F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(27, 18).addBox(-1, 0, -1, 2, 3, 2),
                PartPose.offset(-1, 21, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(27, 18).addBox(-1, 0, -1, 2, 3, 2),
                PartPose.offset(1, 21, 0));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;

        head.xRot = 0.0F;
        head.yRot = 0.0F;
        rightLeg.xRot = 0.0F;
        leftLeg.xRot = 0.0F;

        rightArm.zRot = 1.047198F;
        leftArm.zRot = -1.047198F;
        rightArm.xRot = (-0.2F + 1.5F * triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
        leftArm.xRot = (-0.2F - 1.5F * triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
    }

    public static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }
}
