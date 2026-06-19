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
 * Shade of Leonard model — a faithful port of source {@code ModelLeonard} (1.7.10-0.24.1): a
 * horned demonic goat-shade with a snout, beard, two ears, three horns, a long gown over the
 * legs, and the source attack-timer arm raise. 64×64 texture ({@code leonard.png}).
 */
public class LeonardModel extends EntityModel<LeonardModel.LeonardRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "leonard"), "main");

    private final ModelPart neck;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public LeonardModel(ModelPart root) {
        super(root);
        this.neck     = root.getChild("neck");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Neck → head subtree (the head-look pivot).
        PartDefinition neck = root.addOrReplaceChild("neck",
                CubeListBuilder.create().mirror().texOffs(48, 0).addBox(-2.0F, -1.0F, -2.0F, 4, 2, 4),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-3.0F, -5.0F, -1.0F, 6, 4, 4),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));

        head.addOrReplaceChild("snout",
                CubeListBuilder.create().mirror().texOffs(16, 2).addBox(-2.0F, -5.0F, -7.0F, 4, 4, 7),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));
        head.addOrReplaceChild("beard",
                CubeListBuilder.create().mirror().texOffs(0, 10).addBox(-2.0F, -0.2F, -7.0F, 4, 2, 2),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0113601F, 0.0F, 0.0F));
        head.addOrReplaceChild("ear_left",
                CubeListBuilder.create().mirror().texOffs(38, 0).addBox(3.5F, 1.0F, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5129616F, (float) (-Math.PI / 12), -1.180008F));
        head.addOrReplaceChild("ear_right",
                CubeListBuilder.create().mirror().texOffs(38, 0).addBox(-4.5F, 1.0F, 0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3346075F, 0.0371786F, 1.226894F));
        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create().mirror().texOffs(43, 0).addBox(-0.5F, -12.0F, -0.5F, 1, 8, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2268928F, 0.0F, 0.3665191F));
        head.addOrReplaceChild("horn_middle",
                CubeListBuilder.create().mirror().texOffs(43, 0).addBox(-0.5F, -10.0F, -0.5F, 1, 6, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2974289F, 0.0F, 0.0F));
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create().mirror().texOffs(43, 0).addBox(-0.5F, -12.0F, -0.5F, 1, 8, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2268928F, 0.0F, -0.3665191F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Legs carry the gown skirt as a child, matching the source nesting/offsets.
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("gown_lower_right",
                CubeListBuilder.create().mirror().texOffs(0, 33).addBox(-5.0F, 12.0F, -2.5F, 5, 11, 5),
                PartPose.offset(2.0F, -12.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
                PartPose.offset(2.0F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("gown_lower_left",
                CubeListBuilder.create().mirror().texOffs(21, 33).addBox(0.0F, 12.0F, -2.5F, 5, 11, 5),
                PartPose.offset(-2.0F, -12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LeonardRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbAmt   = state.walkAnimationSpeed;
        float age       = state.ageInTicks;

        // The neck drives the head-look (head + horns + snout follow as children).
        neck.yRot = state.yRot * Mth.DEG_TO_RAD;
        neck.xRot = state.xRot * Mth.DEG_TO_RAD;

        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 2.0F * limbAmt * 0.5F;
        leftArm.xRot  = Mth.cos(limbSwing * 0.6662F)          * 2.0F * limbAmt * 0.5F;
        rightArm.zRot = 0.0F;
        leftArm.zRot  = 0.0F;

        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F)          * 1.4F * limbAmt;
        leftLeg.xRot  = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbAmt;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot  = 0.0F;

        // Idle hover sway on the arms (source func_78087_a tail).
        rightArm.zRot += Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        leftArm.zRot  -= Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        rightArm.xRot += Mth.sin(age * 0.067F) * 0.05F;
        leftArm.xRot  -= Mth.sin(age * 0.067F) * 0.05F;

        // Attack swing — source raises the right arm hard while the attack timer runs.
        if (state.attackTimer > 0) {
            float di = 10.0F;
            float i = state.attackTimer;
            rightArm.xRot = -2.0F + 1.5F * (Math.abs(i % 10.0F - di * 0.5F) - di * 0.25F) / (di * 0.25F);
        }
    }

    public static class LeonardRenderState extends LivingEntityRenderState {
        public int attackTimer;
    }
}
