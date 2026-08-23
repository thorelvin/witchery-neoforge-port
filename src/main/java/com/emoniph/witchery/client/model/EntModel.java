package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Ent model ported from the original 1.7 ModelEnt.
 */
public class EntModel extends EntityModel<EntModel.EntRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "ent"), "main");

    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart face;
    private final ModelPart[] legs = new ModelPart[8];

    public static class EntRenderState extends LivingEntityRenderState {
        public float attackTimer;
        public boolean screaming;
    }

    public EntModel(ModelPart root) {
        super(root);
        this.leftArm = root.getChild("arm_left");
        this.rightArm = root.getChild("arm_right");
        this.face = root.getChild("face");
        for (int i = 0; i < 8; i++) {
            this.legs[i] = root.getChild("leg" + (i + 1));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().mirror().texOffs(82, 0).addBox(0, -22, -3, 6, 24, 6),
                PartPose.offset(8, -4, 0));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().mirror().texOffs(82, 0).addBox(-6, -22, -3, 6, 24, 6),
                PartPose.offset(-8, -4, 0));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(0, 50).addBox(-8, -46, -8, 16, 48, 16),
                PartPose.offset(0, 20, 0));
        root.addOrReplaceChild("face",
                CubeListBuilder.create().mirror().texOffs(0, 116).addBox(-8, -46, -9, 16, 24, 16),
                PartPose.offset(0, 20, 0));

        root.addOrReplaceChild("leg8",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, -1, 0, 0.5759587f, 0.1919862f));
        root.addOrReplaceChild("leg6",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, 0, 0, 0.2792527f, 0.1919862f));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, 1, 0, -0.2792527f, 0.1919862f));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, 2, 0, -0.5759587f, 0.1919862f));
        root.addOrReplaceChild("leg7",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, -1, 0, -0.5759587f, -0.1919862f));
        root.addOrReplaceChild("leg5",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, 0, 0, -0.2792527f, -0.1919862f));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, 1, 0, 0.2792527f, -0.1919862f));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().mirror().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, 2, 0, 0.5759587f, -0.1919862f));

        root.addOrReplaceChild("leaves_base",
                CubeListBuilder.create().mirror().texOffs(0, 180).addBox(0, 0, 0, 60, 16, 60),
                PartPose.offset(-30, -42, -30));
        root.addOrReplaceChild("leaves_top",
                CubeListBuilder.create().mirror().texOffs(56, 130).addBox(0, 0, 0, 32, 16, 32),
                PartPose.offset(-16, -58, -16));
        root.addOrReplaceChild("leaves_base_inner",
                CubeListBuilder.create().mirror().texOffs(24, 59)
                        .addBox(0, 0, 0, 56, 14, 56, CubeDeformation.NONE, 0.25F, 0.125F),
                PartPose.offset(-28, -41, -28));
        root.addOrReplaceChild("leaves_top_inner",
                CubeListBuilder.create().mirror().texOffs(108, 14)
                        .addBox(0, 0, 0, 28, 14, 28, CubeDeformation.NONE, 0.25F, 0.125F),
                PartPose.offset(-14, -57, -14));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(EntRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        face.visible = state.screaming;

        float attackTimer = state.attackTimer;
        if (attackTimer > 0.0f) {
            rightArm.xRot = 3.0f - 1.3f * triangleWave(attackTimer, 10.0f);
            leftArm.xRot = 2.5f - 1.2f * triangleWave(attackTimer, 10.0f);
        } else {
            rightArm.xRot = 0.0f;
            leftArm.xRot = 0.0f;
            rightArm.zRot = (-0.2f + 0.1f * triangleWave(limbSwing, 13.0f)) * limbSwingAmount - 0.1f;
            leftArm.zRot = (0.2f - 0.1f * triangleWave(limbSwing, 13.0f)) * limbSwingAmount + 0.1f;
        }

        animateRootLegs(limbSwing, limbSwingAmount);
    }

    private void animateRootLegs(float limbSwing, float limbSwingAmount) {
        float quarterPi = Mth.PI / 4.0f;
        legs[0].zRot = -quarterPi;
        legs[1].zRot = quarterPi;
        legs[2].zRot = -quarterPi * 0.74f;
        legs[3].zRot = quarterPi * 0.74f;
        legs[4].zRot = -quarterPi * 0.74f;
        legs[5].zRot = quarterPi * 0.74f;
        legs[6].zRot = -quarterPi;
        legs[7].zRot = quarterPi;

        float eighthPi = Mth.PI / 8.0f;
        legs[0].yRot = eighthPi * 2.0f;
        legs[1].yRot = -eighthPi * 2.0f;
        legs[2].yRot = eighthPi;
        legs[3].yRot = -eighthPi;
        legs[4].yRot = -eighthPi;
        legs[5].yRot = eighthPi;
        legs[6].yRot = -eighthPi * 2.0f;
        legs[7].yRot = eighthPi * 2.0f;

        float y1 = -(Mth.cos(limbSwing * 0.6662f * 2.0f) * 0.4f) * limbSwingAmount;
        float y2 = -(Mth.cos(limbSwing * 0.6662f * 2.0f + Mth.PI) * 0.4f) * limbSwingAmount;
        float y3 = -(Mth.cos(limbSwing * 0.6662f * 2.0f + Mth.HALF_PI) * 0.4f) * limbSwingAmount;
        float y4 = -(Mth.cos(limbSwing * 0.6662f * 2.0f + Mth.PI * 1.5f) * 0.4f) * limbSwingAmount;
        float z1 = Math.abs(Mth.sin(limbSwing * 0.6662f) * 0.4f) * limbSwingAmount;
        float z2 = Math.abs(Mth.sin(limbSwing * 0.6662f + Mth.PI) * 0.4f) * limbSwingAmount;
        float z3 = Math.abs(Mth.sin(limbSwing * 0.6662f + Mth.HALF_PI) * 0.4f) * limbSwingAmount;
        float z4 = Math.abs(Mth.sin(limbSwing * 0.6662f + Mth.PI * 1.5f) * 0.4f) * limbSwingAmount;

        legs[0].yRot += y1;
        legs[1].yRot += -y1;
        legs[2].yRot += y2;
        legs[3].yRot += -y2;
        legs[4].yRot += y3;
        legs[5].yRot += -y3;
        legs[6].yRot += y4;
        legs[7].yRot += -y4;
        legs[0].zRot += z1;
        legs[1].zRot += -z1;
        legs[2].zRot += z2;
        legs[3].zRot += -z2;
        legs[4].zRot += z3;
        legs[5].zRot += -z3;
        legs[6].zRot += z4;
        legs[7].zRot += -z4;
    }

    private static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5f) - period * 0.25f) / (period * 0.25f);
    }
}
