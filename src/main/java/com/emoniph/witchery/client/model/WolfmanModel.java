package com.emoniph.witchery.client.model;

import com.emoniph.witchery.client.renderer.WolfmanRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Source-exact port of Witchery 0.24.1 {@code ModelWolfman}. */
public class WolfmanModel extends EntityModel<WolfmanRenderer.WolfmanRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "wolfman_entity"), "main");

    private static final float BODY_X_ROT = 0.4098033F;
    private static final float LEG_X_ROT = -0.4098033F;
    private static final float TAIL_X_ROT = 0.59184116F;

    private final ModelPart headMain;
    private final ModelPart bodyUpper;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeftUpper;
    private final ModelPart legRightUpper;
    private final ModelPart tail;

    public WolfmanModel(ModelPart root) {
        super(root);
        this.headMain = root.getChild("head_main");
        this.bodyUpper = root.getChild("body_upper");
        this.armLeft = root.getChild("arm_left");
        this.armRight = root.getChild("arm_right");
        this.legLeftUpper = root.getChild("leg_left_upper");
        this.legRightUpper = root.getChild("leg_right_upper");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 6.0F, 4.0F,
                                new CubeDeformation(0.05F))
                        .texOffs(16, 14).addBox(-3.0F, -8.0F, 1.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(16, 14).addBox(1.0F, -8.0F, 1.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(0, 10).addBox(-1.5F, -3.1F, -5.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));

        PartDefinition bodyUpper = root.addOrReplaceChild("body_upper",
                CubeListBuilder.create().texOffs(0, 35)
                        .addBox(-5.0F, 0.0F, -3.9F, 10.0F, 7.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -0.1F, -2.0F, BODY_X_ROT, 0.0F, 0.0F));
        bodyUpper.addOrReplaceChild("body_lower",
                CubeListBuilder.create().texOffs(3, 50)
                        .addBox(-4.0F, 2.0F, -2.3F, 8.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 5.0F, -1.5F));

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(55, 52)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 11.9F, 3.6F, TAIL_X_ROT, 0.0F, 0.0F));

        PartDefinition legLeftUpper = root.addOrReplaceChild("leg_left_upper",
                CubeListBuilder.create().texOffs(38, 0).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, LEG_X_ROT, 0.0F, 0.0F));
        legLeftUpper.addOrReplaceChild("leg_left_lower",
                CubeListBuilder.create().texOffs(38, 13)
                        .addBox(-2.0F, 3.5F, 2.0F, 4.0F, 8.0F, 4.0F),
                PartPose.ZERO);

        PartDefinition legRightUpper = root.addOrReplaceChild("leg_right_upper",
                CubeListBuilder.create().texOffs(38, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, LEG_X_ROT, 0.0F, 0.0F));
        legRightUpper.addOrReplaceChild("leg_right_lower",
                CubeListBuilder.create().texOffs(38, 13)
                        .addBox(-2.0F, 3.5F, 2.0F, 4.0F, 8.0F, 4.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(38, 46).mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(6.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(38, 46)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(-5.8F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WolfmanRenderer.WolfmanRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float age = state.ageInTicks;

        headMain.x = 0.0F;
        headMain.y = 0.0F;
        headMain.z = -2.0F;
        headMain.xRot = state.xRot * Mth.DEG_TO_RAD;
        headMain.yRot = state.yRot * Mth.DEG_TO_RAD;
        headMain.zRot = 0.0F;

        bodyUpper.x = 0.0F;
        bodyUpper.y = -0.1F;
        bodyUpper.z = -2.0F;
        bodyUpper.xRot = BODY_X_ROT;
        bodyUpper.yRot = 0.0F;
        bodyUpper.zRot = 0.0F;

        armLeft.x = 6.0F;
        armLeft.y = 2.0F;
        armLeft.z = 0.0F;
        armLeft.yRot = 0.0F;
        armLeft.zRot = 0.0F;
        armRight.x = -5.8F;
        armRight.y = 2.0F;
        armRight.z = 0.0F;
        armRight.yRot = 0.0F;
        armRight.zRot = 0.0F;

        if (state.attackTimer > 0.0F) {
            armRight.xRot = -2.0F + 1.5F * triangleWave(state.attackTimer, 10.0F);
            armLeft.xRot = -1.0F + 0.9F * triangleWave(state.attackTimer, 10.0F);
        } else {
            armRight.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbAmount;
            armLeft.xRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
        }

        legRightUpper.x = -2.0F;
        legRightUpper.y = 12.0F;
        legRightUpper.z = 0.1F;
        legRightUpper.xRot = Math.max(LEG_X_ROT
                + Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount, -0.8F);
        legRightUpper.yRot = 0.0F;
        legRightUpper.zRot = 0.0F;
        legLeftUpper.x = 2.0F;
        legLeftUpper.y = 12.0F;
        legLeftUpper.z = 0.1F;
        legLeftUpper.xRot = Math.max(LEG_X_ROT
                + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbAmount, -0.8F);
        legLeftUpper.yRot = 0.0F;
        legLeftUpper.zRot = 0.0F;

        if (state.sourceSitting) {
            armRight.xRot -= Mth.PI / 5.0F;
            armLeft.xRot -= Mth.PI / 5.0F;
            legRightUpper.xRot = -Mth.PI * 2.0F / 5.0F;
            legLeftUpper.xRot = -Mth.PI * 2.0F / 5.0F;
            legRightUpper.yRot = Mth.PI / 10.0F;
            legLeftUpper.yRot = -Mth.PI / 10.0F;
        }

        if (state.heldItemLeft != 0) {
            armLeft.xRot = armLeft.xRot * 0.5F - Mth.PI / 10.0F * state.heldItemLeft;
        }
        if (state.heldItemRight != 0) {
            armRight.xRot = armRight.xRot * 0.5F - Mth.PI / 10.0F * state.heldItemRight;
        }

        float attackProgress = state.attackProgress;
        if (attackProgress > -9990.0F) {
            float bodyYaw = Mth.sin(Mth.sqrt(attackProgress) * Mth.TWO_PI) * 0.2F;
            bodyUpper.yRot = bodyYaw;
            armRight.z = Mth.sin(bodyYaw) * 5.0F;
            armRight.x = -Mth.cos(bodyYaw) * 5.0F;
            armLeft.z = -Mth.sin(bodyYaw) * 5.0F;
            armLeft.x = Mth.cos(bodyYaw) * 5.0F;
            armRight.yRot += bodyYaw;
            armLeft.yRot += bodyYaw;
            armLeft.xRot += bodyYaw;

            float eased = 1.0F - attackProgress;
            eased *= eased;
            eased *= eased;
            eased = 1.0F - eased;
            float swing = Mth.sin(eased * Mth.PI);
            float headCompensation = Mth.sin(attackProgress * Mth.PI)
                    * -(headMain.xRot - 0.7F) * 0.75F;
            armRight.xRot -= swing * 1.2F + headCompensation;
            armRight.yRot += bodyYaw * 2.0F;
            armRight.zRot = Mth.sin(attackProgress * Mth.PI) * -0.4F;
        }

        if (state.sourceCrouching) {
            bodyUpper.xRot = 0.5F;
            armRight.xRot += 0.4F;
            armLeft.xRot += 0.4F;
            legRightUpper.z = 4.0F;
            legLeftUpper.z = 4.0F;
            legRightUpper.y = 9.0F;
            legLeftUpper.y = 9.0F;
        } else {
            bodyUpper.xRot = BODY_X_ROT;
            bodyUpper.yRot = 0.0F;
            bodyUpper.zRot = 0.0F;
        }

        tail.x = 0.0F;
        tail.y = 11.9F;
        tail.z = 3.6F;
        tail.xRot = TAIL_X_ROT;
        tail.yRot = 0.0F;
        tail.zRot = 0.0F;
        if (limbAmount > 0.1F) {
            tail.xRot += limbAmount - 0.1F;
            tail.zRot += 0.25F * Mth.cos(age * 0.09F) + 0.05F;
        } else {
            tail.zRot += 0.15F * Mth.cos(age * 0.09F) + 0.05F;
        }

        addSourceArmIdle(age);
        if (state.aimedBow) {
            armRight.zRot = 0.0F;
            armLeft.zRot = 0.0F;
            armRight.yRot = -0.1F + headMain.yRot;
            armLeft.yRot = 0.5F + headMain.yRot;
            armRight.xRot = -Mth.HALF_PI + headMain.xRot;
            armLeft.xRot = -Mth.HALF_PI + headMain.xRot;
            addSourceArmIdle(age);
        }
    }

    private void addSourceArmIdle(float age) {
        float roll = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        float pitch = Mth.sin(age * 0.067F) * 0.05F;
        armRight.zRot += roll;
        armLeft.zRot -= roll;
        armRight.xRot += pitch;
        armLeft.xRot -= pitch;
    }

    static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F)
                / (period * 0.25F);
    }
}
