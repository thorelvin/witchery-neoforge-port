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

/** Exact geometry and pose port of the original 1.7.10 ModelGoblinMog. */
public class GoblinMogModel extends EntityModel<GoblinMogModel.MogRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "goblin_mog"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public static class MogRenderState extends LivingEntityRenderState {
        public boolean crouching;
        public boolean holdingRightItem;
        public boolean holdingLeftItem;
        public boolean aimedBow;
        public float swingProgress;
    }

    public GoblinMogModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(40, 14).addBox(-3, -2, -2, 4, 14, 4),
                PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(40, 14).addBox(-1, -2, -2, 4, 14, 4),
                PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(2, 12, 0));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().mirror().texOffs(35, 5).addBox(-4, -2, -5, 8, 4, 4),
                PartPose.offsetAndRotation(0, 2, 0, Mth.PI / 4.0F, 0, 0));
        root.addOrReplaceChild("skirt",
                CubeListBuilder.create().mirror().texOffs(14, 34).addBox(-4.5F, 0, -2.5F, 9, 11, 5),
                PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                        .texOffs(0, 4).addBox(-2, -4, -5, 1, 2, 1)
                        .texOffs(0, 4).addBox(1, -4, -5, 1, 2, 1)
                        .texOffs(25, 0).addBox(-1, -6, -6, 2, 3, 2)
                        .texOffs(34, 0).addBox(-2, -2, -6, 4, 1, 2),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MogRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        float age = state.ageInTicks;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        rightArm.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * speed;
        leftArm.xRot = Mth.cos(walk * 0.6662F) * speed;
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.4F * speed;
        leftLeg.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 1.4F * speed;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;

        if (state.crouching) {
            rightArm.xRot -= Mth.PI / 5.0F;
            leftArm.xRot -= Mth.PI / 5.0F;
            rightLeg.xRot = -Mth.PI * 2.0F / 5.0F;
            leftLeg.xRot = -Mth.PI * 2.0F / 5.0F;
            rightLeg.yRot = Mth.PI / 10.0F;
            leftLeg.yRot = -Mth.PI / 10.0F;
        }
        if (state.holdingLeftItem) leftArm.xRot = leftArm.xRot * 0.5F - Mth.PI / 10.0F;
        if (state.holdingRightItem) rightArm.xRot = rightArm.xRot * 0.5F - Mth.PI / 10.0F;
        applySwing(state.swingProgress);

        if (state.crouching) {
            body.xRot = 0.5F;
            rightArm.xRot += 0.4F;
            leftArm.xRot += 0.4F;
            rightLeg.setPos(-2, 9, 4);
            leftLeg.setPos(2, 9, 4);
            head.y = 1.0F;
        } else {
            body.xRot = 0.0F;
            rightLeg.setPos(-2, 12, 0.1F);
            leftLeg.setPos(2, 12, 0.1F);
            head.y = 0.0F;
        }

        float rollBob = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        float pitchBob = Mth.sin(age * 0.067F) * 0.05F;
        rightArm.zRot += rollBob;
        leftArm.zRot -= rollBob;
        rightArm.xRot += pitchBob;
        leftArm.xRot -= pitchBob;
        if (state.aimedBow) applyBowPose(age);
    }

    private void applySwing(float swingProgress) {
        if (swingProgress <= -9990.0F) return;
        body.yRot = Mth.sin(Mth.sqrt(swingProgress) * Mth.TWO_PI) * 0.2F;
        rightArm.z = Mth.sin(body.yRot) * 5.0F;
        rightArm.x = -Mth.cos(body.yRot) * 5.0F;
        leftArm.z = -Mth.sin(body.yRot) * 5.0F;
        leftArm.x = Mth.cos(body.yRot) * 5.0F;
        rightArm.yRot += body.yRot;
        leftArm.yRot += body.yRot;
        leftArm.xRot += body.yRot;
        float eased = 1.0F - swingProgress;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float strike = Mth.sin(eased * Mth.PI);
        float headCompensation = Mth.sin(swingProgress * Mth.PI) * -(head.xRot - 0.7F) * 0.75F;
        rightArm.xRot -= strike * 1.2F + headCompensation;
        rightArm.yRot += body.yRot * 2.0F;
        rightArm.zRot = Mth.sin(swingProgress * Mth.PI) * -0.4F;
    }

    private void applyBowPose(float age) {
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.yRot = -0.1F + head.yRot;
        leftArm.yRot = 0.1F + head.yRot + 0.4F;
        rightArm.xRot = -Mth.HALF_PI + head.xRot;
        leftArm.xRot = -Mth.HALF_PI + head.xRot;
        float rollBob = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        float pitchBob = Mth.sin(age * 0.067F) * 0.05F;
        rightArm.zRot += rollBob;
        leftArm.zRot -= rollBob;
        rightArm.xRot += pitchBob;
        leftArm.xRot -= pitchBob;
    }
}
