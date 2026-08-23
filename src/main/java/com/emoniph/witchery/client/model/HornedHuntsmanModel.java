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
import net.minecraft.util.Mth;

/** Exact port of Witchery 1.7.10's {@code ModelHornedAvatar}. */
public final class HornedHuntsmanModel extends EntityModel<HornedHuntsmanModel.HuntsmanRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "horned_huntsman"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart rightShin;
    private final ModelPart rightFoot;
    private final ModelPart leftLeg;
    private final ModelPart leftShin;
    private final ModelPart leftFoot;

    public HornedHuntsmanModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.rightShin = rightLeg.getChild("right_shin");
        this.rightFoot = rightShin.getChild("right_foot");
        this.leftLeg = root.getChild("left_leg");
        this.leftShin = leftLeg.getChild("left_shin");
        this.leftFoot = leftShin.getChild("left_foot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(4, 112)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -16.0F, 0.0F));
        head.addOrReplaceChild("horns",
                CubeListBuilder.create().texOffs(0, 88)
                        .addBox(-10.0F, -24.0F, 0.0F, 20.0F, 17.0F, 0.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(12, 61)
                        .addBox(-6.0F, 0.0F, -3.0F, 12.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 43)
                        .addBox(0.0F, 0.0F, 0.0F, 20.0F, 8.0F, 10.0F),
                PartPose.offset(-10.0F, -16.0F, -5.0F));
        root.addOrReplaceChild("hips",
                CubeListBuilder.create().texOffs(8, 75)
                        .addBox(-7.0F, 0.0F, -4.0F, 14.0F, 4.0F, 8.0F),
                PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(72, 50)
                        .addBox(-4.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(-10.0F, -13.0F, 0.0F));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(68, 67)
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offsetAndRotation(-2.0F, 10.0F, 0.0F,
                        -(float) Math.PI / 6.0F, 0.0F, 0.0F));
        rightForearm.addOrReplaceChild("spear",
                CubeListBuilder.create().mirror()
                        .texOffs(61, 14).addBox(-0.5F, -30.0F, -0.5F, 1.0F, 50.0F, 1.0F)
                        .texOffs(60, 8).addBox(-1.5F, -35.0F, 0.0F, 3.0F, 6.0F, 0.0F)
                        .texOffs(60, 5).addBox(0.0F, -35.0F, -1.5F, 0.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 1.5F, 0.0F, 0.0F));

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(72, 50)
                        .addBox(0.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(10.0F, -13.0F, 0.0F));
        leftArm.addOrReplaceChild("left_forearm",
                CubeListBuilder.create().texOffs(68, 67)
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offsetAndRotation(2.0F, 10.0F, 0.0F,
                        -(float) Math.PI / 6.0F, 0.0F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(72, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(-4.0F, 3.0F, -1.0F,
                        (float) Math.PI / 6.0F, 0.0F, 0.0F));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(68, 14)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.9F, 0.0F, 0.0F));
        rightShin.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(69, 34)
                        .addBox(-2.0F, 0.0F, -5.0F, 4.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.5F, 0.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(72, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(4.0F, 3.0F, -1.0F,
                        (float) Math.PI / 6.0F, 0.0F, 0.0F));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(68, 14)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.9F, 0.0F, 0.0F));
        leftShin.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(69, 34)
                        .addBox(-2.0F, 0.0F, -5.0F, 4.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.5F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HuntsmanRenderState state) {
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float gait = triangleWave(state.walkAnimationPos, 13.0F) * state.walkAnimationSpeed;
        this.leftLeg.xRot = -1.3F * gait + 0.5F;
        this.rightLeg.xRot = 1.3F * gait + 0.5F;
        this.leftShin.xRot = 0.8F * (this.rightLeg.xRot - 0.5F) - 1.0F;
        this.rightShin.xRot = 0.8F * (this.leftLeg.xRot - 0.5F) - 1.0F;
        this.leftFoot.xRot = Math.max(1.4F * (this.leftLeg.xRot - 0.5F) + 0.5F, 0.2F);
        this.rightFoot.xRot = Math.max(1.4F * (this.rightLeg.xRot - 0.5F) + 0.5F, 0.2F);
        this.leftLeg.yRot = 0.0F;
        this.rightLeg.yRot = 0.0F;

        float armWave = triangleWave(state.walkAnimationPos, 13.0F);
        this.leftArm.xRot = (-0.2F - 1.5F * armWave) * state.walkAnimationSpeed * 0.2F;
        this.rightArm.xRot = state.attackTimer > 0
                ? -2.0F + 1.5F * triangleWave(state.attackTimer - state.partialTick, 10.0F)
                : (-0.2F + 1.5F * armWave) * state.walkAnimationSpeed * 0.2F;
    }

    public static float triangleWave(float angle, float limit) {
        return (Math.abs(angle % limit - limit * 0.5F) - limit * 0.25F) / (limit * 0.25F);
    }

    public static final class HuntsmanRenderState extends LivingEntityRenderState {
        public int attackTimer;
    }
}
