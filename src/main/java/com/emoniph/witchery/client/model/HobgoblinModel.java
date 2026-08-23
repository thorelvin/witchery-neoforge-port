package com.emoniph.witchery.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Exact geometry and pose port of the original 1.7.10 ModelGoblin. */
public class HobgoblinModel extends HumanoidModel<HobgoblinModel.HobgoblinRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "hobgoblin"), "main");

    public static class HobgoblinRenderState extends HumanoidRenderState {
        public int profession;
        public boolean sourceCrouching;
        public boolean worshipping;
        public boolean working;
        public boolean workingWithKoboldite;
        public boolean holdingRightItem;
        public boolean holdingLeftItem;
        public boolean aimedBow;
        public float swingProgress;
        public int entityTick;
    }

    public HobgoblinModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                        .texOffs(34, 3).addBox(-0.5F, -6, -5, 1, 3, 1)
                        .texOffs(34, 0).addBox(-0.5F, -5, -6, 1, 1, 1)
                        .texOffs(33, 9).addBox(-0.5F, -4, -7, 1, 2, 2)
                        .texOffs(46, 0).addBox(6, -7, 0, 2, 2, 1)
                        .texOffs(39, 0).addBox(4, -7, 0, 2, 3, 1)
                        .texOffs(39, 0).addBox(-6, -7, 0, 2, 3, 1)
                        .texOffs(46, 0).addBox(-8, -7, 0, 2, 2, 1),
                PartPose.offset(0, 11, 0));
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.ZERO);
        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(16, 16).addBox(-4, 0, -2, 8, 7, 4),
                PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-3, -3, -2, 4, 12, 4),
                PartPose.offset(-5, 12, 0));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1, -3, -2, 4, 12, 4),
                PartPose.offset(5, 12, 0));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2, 0, -2, 4, 6, 4),
                PartPose.offset(-2, 18, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2, 0, -2, 4, 6, 4),
                PartPose.offset(2, 18, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HobgoblinRenderState state) {
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

        if (state.sourceCrouching) {
            rightArm.xRot -= Mth.PI / 5.0F;
            leftArm.xRot -= Mth.PI / 5.0F;
            rightLeg.xRot = -Mth.PI * 2.0F / 5.0F;
            leftLeg.xRot = -Mth.PI * 2.0F / 5.0F;
            rightLeg.yRot = Mth.PI / 10.0F;
            leftLeg.yRot = -Mth.PI / 10.0F;
        }
        if (state.holdingLeftItem) leftArm.xRot = leftArm.xRot * 0.5F - Mth.PI / 10.0F;
        if (state.holdingRightItem) {
            rightArm.xRot = rightArm.xRot * 0.5F - Mth.PI / 10.0F;
            if (state.working) {
                rightArm.xRot -= state.workingWithKoboldite
                        ? (state.entityTick % 6) * 0.3F
                        : (state.entityTick % 20) * 0.1F;
            }
        }
        applySwing(state.swingProgress);

        if (!state.sourceCrouching && !state.worshipping) {
            body.xRot = 0.0F;
            rightLeg.setPos(-2, 18, 0.1F);
            leftLeg.setPos(2, 18, 0.1F);
            head.y = 11.0F;
            body.y = 11.0F;
        } else {
            body.xRot = 0.5F;
            rightArm.xRot -= 2.2F;
            leftArm.xRot -= 2.2F;
            rightLeg.setPos(-2, 18, 3.0F);
            leftLeg.setPos(2, 18, 3.0F);
            head.xRot = 0.5F;
            head.y = 13.0F;
            body.y = 13.0F;
        }

        float rollBob = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        float pitchBob = Mth.sin(age * 0.067F) * 0.05F;
        rightArm.zRot += rollBob;
        leftArm.zRot -= rollBob;
        rightArm.xRot += pitchBob;
        leftArm.xRot -= pitchBob;
        if (state.aimedBow) applyBowPose(age);
        hat.visible = false;
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
