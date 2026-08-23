package com.emoniph.witchery.client.model;

import com.emoniph.witchery.WitcheryMod;
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

/** Exact 1.7.10 {@code ModelWolf} used by Witchery's player WOLF transform. */
public final class WerewolfWolfModel extends EntityModel<WerewolfWolfModel.WolfRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "werewolf_wolf"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart upperBody;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;

    public WerewolfWolfModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.upperBody = root.getChild("upper_body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F)
                        .texOffs(16, 14).addBox(-3.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(16, 14).addBox(1.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(0, 10).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(-1.0F, 13.5F, -7.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(18, 14)
                        .addBox(-4.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 14.0F, 2.0F));
        root.addOrReplaceChild("upper_body",
                CubeListBuilder.create().texOffs(21, 0)
                        .addBox(-4.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F),
                PartPose.offset(-1.0F, 14.0F, 2.0F));

        root.addOrReplaceChild("right_hind_leg", sourceLeg(),
                PartPose.offset(-2.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", sourceLeg(),
                PartPose.offset(0.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", sourceLeg(),
                PartPose.offset(-2.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", sourceLeg(),
                PartPose.offset(0.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(9, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-1.0F, 12.0F, 8.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static CubeListBuilder sourceLeg() {
        return CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F);
    }

    @Override
    public void setupAnim(WolfRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        head.x = -1.0F;
        head.y = 13.5F;
        head.z = -7.0F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.zRot = state.interestedAngle + state.headShakeAngle;

        tail.yRot = state.angry
                ? 0.0F
                : Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        tail.xRot = state.ageInTicks;
        tail.zRot = state.tailShakeAngle;

        if (state.sitting) {
            upperBody.x = -1.0F;
            upperBody.y = 16.0F;
            upperBody.z = -3.0F;
            upperBody.xRot = 1.2566371F;
            upperBody.yRot = 0.0F;
            upperBody.zRot = state.upperBodyShakeAngle;
            body.x = 0.0F;
            body.y = 18.0F;
            body.z = 0.0F;
            body.xRot = 0.7853982F;
            body.yRot = 0.0F;
            body.zRot = state.bodyShakeAngle;
            tail.x = -1.0F;
            tail.y = 21.0F;
            tail.z = 6.0F;
            setLegPose(rightHindLeg, -2.5F, 22.0F, 2.0F, 4.712389F);
            setLegPose(leftHindLeg, 0.5F, 22.0F, 2.0F, 4.712389F);
            setLegPose(rightFrontLeg, -2.49F, 17.0F, -4.0F, 5.811947F);
            setLegPose(leftFrontLeg, 0.51F, 17.0F, -4.0F, 5.811947F);
        } else {
            body.x = 0.0F;
            body.y = 14.0F;
            body.z = 2.0F;
            body.xRot = Mth.HALF_PI;
            body.yRot = 0.0F;
            body.zRot = state.bodyShakeAngle;
            upperBody.x = -1.0F;
            upperBody.y = 14.0F;
            upperBody.z = -3.0F;
            upperBody.xRot = body.xRot;
            upperBody.yRot = 0.0F;
            upperBody.zRot = state.upperBodyShakeAngle;
            tail.x = -1.0F;
            tail.y = 12.0F;
            tail.z = 8.0F;
            float hindRight = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
            float hindLeft = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbAmount;
            setLegPose(rightHindLeg, -2.5F, 16.0F, 7.0F, hindRight);
            setLegPose(leftHindLeg, 0.5F, 16.0F, 7.0F, hindLeft);
            setLegPose(rightFrontLeg, -2.5F, 16.0F, -4.0F, hindLeft);
            setLegPose(leftFrontLeg, 0.5F, 16.0F, -4.0F, hindRight);
        }
    }

    private static void setLegPose(ModelPart leg, float x, float y, float z, float xRot) {
        leg.x = x;
        leg.y = y;
        leg.z = z;
        leg.xRot = xRot;
        leg.yRot = 0.0F;
        leg.zRot = 0.0F;
    }

    public static class WolfRenderState extends LivingEntityRenderState {
        public boolean angry;
        public boolean sitting;
        public boolean sourceCrouching;
        public float interestedAngle;
        public float headShakeAngle;
        public float upperBodyShakeAngle;
        public float bodyShakeAngle;
        public float tailShakeAngle;
    }
}
