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
 * Lilith boss model. Ported from 1.7.10 ModelLilith (64×64 texture).
 * Features segmented arms/legs, wings, horns, and a skirt.
 */
public class LilithModel extends EntityModel<LilithModel.LilithRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "lilith"), "main");

    private final ModelPart neck;
    private final ModelPart bodyChest;
    private final ModelPart bodyWaist;
    private final ModelPart bodyShoulders;
    private final ModelPart armRight;
    private final ModelPart armLeft;
    private final ModelPart legRight;
    private final ModelPart legLeft;
    private final ModelPart head;
    private final ModelPart skirt1;
    private final ModelPart skirt2;

    public LilithModel(ModelPart root) {
        super(root);
        this.neck          = root.getChild("neck");
        this.bodyChest     = root.getChild("body_chest");
        this.bodyWaist     = root.getChild("body_waist");
        this.bodyShoulders = root.getChild("body_shoulders");
        this.armRight      = root.getChild("arm_right");
        this.armLeft       = root.getChild("arm_left");
        this.legRight      = root.getChild("leg_right");
        this.legLeft       = root.getChild("leg_left");
        this.head          = root.getChild("head");
        this.skirt1        = root.getChild("skirt1");
        this.skirt2        = root.getChild("skirt2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-1.5f, -1.5f, -1.5f, 3, 2, 3),
                PartPose.offset(0, -13, 0));

        root.addOrReplaceChild("body_chest",
                CubeListBuilder.create().texOffs(17, 17)
                        .addBox(-4, -1.5f, -1.5f, 8, 3, 3),
                PartPose.offsetAndRotation(0, -9.8f, -1.9f, (float) (Math.PI / 4), 0, 0));

        root.addOrReplaceChild("body_waist",
                CubeListBuilder.create().texOffs(20, 24)
                        .addBox(-3, 0, -1, 6, 10, 2),
                PartPose.offset(0, -7.5f, 0));

        root.addOrReplaceChild("body_shoulders",
                CubeListBuilder.create().texOffs(15, 6)
                        .addBox(-4, 0, -2, 8, 6, 4),
                PartPose.offset(0, -12.7f, 0));

        // Right arm
        PartDefinition armRight = root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.5f, -1.5f, -1.5f, 3, 13, 3),
                PartPose.offset(-4.5f, -11.5f, 0));
        armRight.addOrReplaceChild("arm_right_lower",
                CubeListBuilder.create().texOffs(8, 25)
                        .addBox(-2.5f, 9.8f, 0.8f, 3, 13, 3),
                PartPose.offsetAndRotation(0, 0, 0, -0.22759093f, 0, 0));
        armRight.addOrReplaceChild("arm_right_wing",
                CubeListBuilder.create().texOffs(0, 13)
                        .addBox(-1, -19.6f, -12.7f, 1, 30, 4),
                PartPose.offsetAndRotation(0, 0, 0, 2.5497515f, (float) (-Math.PI / 18), 0));

        // Left arm
        PartDefinition armLeft = root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(-0.5f, -1.5f, -1.5f, 3, 13, 3),
                PartPose.offset(4.4f, -11.5f, 0));
        armLeft.addOrReplaceChild("arm_left_lower",
                CubeListBuilder.create().texOffs(8, 25).mirror()
                        .addBox(-0.5f, 9.8f, 0.8f, 3, 13, 3),
                PartPose.offsetAndRotation(0, 0, 0, -0.22759093f, 0, 0));
        armLeft.addOrReplaceChild("arm_left_wing",
                CubeListBuilder.create().texOffs(0, 13).mirror()
                        .addBox(1, -19.6f, -12.7f, 1, 30, 4),
                PartPose.offsetAndRotation(0, 0, 0, 2.5497515f, (float) (Math.PI / 18), 0));

        // Right leg
        PartDefinition legRight = root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(36, 30)
                        .addBox(-2, 0, -2, 4, 13, 4),
                PartPose.offsetAndRotation(-2.1f, 2.5f, 0, -0.27314404f, 0, 0));
        legRight.addOrReplaceChild("leg_right_lower",
                CubeListBuilder.create().texOffs(48, 47)
                        .addBox(-2, 8, 2, 4, 13, 4),
                PartPose.ZERO);

        // Left leg
        PartDefinition legLeft = root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(36, 30).mirror()
                        .addBox(-2, 0, -2, 4, 13, 4),
                PartPose.offsetAndRotation(2.1f, 2.5f, 0, -0.27314404f, 0, 0));
        legLeft.addOrReplaceChild("leg_left_lower",
                CubeListBuilder.create().texOffs(48, 47).mirror()
                        .addBox(-2, 8, 2, 4, 13, 4),
                PartPose.ZERO);

        // Head
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-3, -6, -3, 6, 6, 6),
                PartPose.offset(0, -13.5f, 0));
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(52, 30)
                        .addBox(-7, -12.3f, 0, 6, 10, 1),
                PartPose.offsetAndRotation(0, 0, 0, -0.18203785f, 0, 0));
        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(52, 30).mirror()
                        .addBox(1, -12.3f, 0, 6, 10, 1),
                PartPose.offsetAndRotation(0, 0, 0, -0.18203785f, 0, 0));
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(41, 0)
                        .addBox(-0.5f, -3.6f, -4, 1, 2, 1),
                PartPose.ZERO);
        head.addOrReplaceChild("tooth_right",
                CubeListBuilder.create().texOffs(20, 0)
                        .addBox(-1.5f, -1.6f, -3.6f, 1, 3, 1, new CubeDeformation(-0.35f)),
                PartPose.ZERO);
        head.addOrReplaceChild("tooth_left",
                CubeListBuilder.create().texOffs(20, 0).mirror()
                        .addBox(0.5f, -1.6f, -3.6f, 1, 3, 1, new CubeDeformation(-0.35f)),
                PartPose.ZERO);
        PartDefinition head2 = head.addOrReplaceChild("head2",
                CubeListBuilder.create().texOffs(42, 12)
                        .addBox(-2.5f, -5.5f, 1, 5, 5, 5),
                PartPose.offsetAndRotation(0, 0, 0, -0.18203785f, 0, 0));
        head2.addOrReplaceChild("head3",
                CubeListBuilder.create().texOffs(44, 22)
                        .addBox(-2, -4.7f, 5.6f, 4, 4, 4),
                PartPose.ZERO);

        // Skirts
        root.addOrReplaceChild("skirt1",
                CubeListBuilder.create().texOffs(0, 49)
                        .addBox(-4.5f, 0, -2.5f, 9, 10, 5),
                PartPose.offset(0, -0.9f, 0));
        root.addOrReplaceChild("skirt2",
                CubeListBuilder.create().texOffs(0, 49)
                        .addBox(-4.5f, 0, -2.5f, 9, 10, 5),
                PartPose.offset(0, -0.9f, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LilithRenderState state) {
        float limbSwing    = state.walkAnimationPos;
        float limbSwingAmt = state.walkAnimationSpeed;
        float ageInTicks   = state.ageInTicks;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        armRight.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmt * 0.5F;
        armLeft.xRot  = Mth.cos(limbSwing * 0.6662F)           * 2.0F * limbSwingAmt * 0.5F;
        armRight.zRot = 0;
        armLeft.zRot  = 0;

        float legSwingR = Math.max(Mth.cos(limbSwing * 0.6662F)          * 1.4F * limbSwingAmt - 0.27314404f, -0.8f);
        float legSwingL = Math.max(Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmt - 0.27314404f, -0.8f);
        legRight.xRot = legSwingR;
        legLeft.xRot  = legSwingL;
        legRight.yRot = 0;
        legLeft.yRot  = 0;

        skirt1.xRot = Math.min(legSwingR, legSwingL);
        skirt2.xRot = Math.max(Math.max(legSwingR, legSwingL), 0.2f);

        armRight.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        armLeft.zRot  -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        armRight.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        armLeft.xRot  -= Mth.sin(ageInTicks * 0.067F) * 0.05F;

        if (state.attackTimer > 0) {
            armRight.xRot = -2.0F;
        }
    }

    public static class LilithRenderState extends LivingEntityRenderState {
        public int attackTimer;
        public boolean isFriendly;
    }
}
