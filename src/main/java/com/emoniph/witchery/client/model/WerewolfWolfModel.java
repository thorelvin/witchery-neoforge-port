package com.emoniph.witchery.client.model;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Vanilla-shaped quadruped proxy used by players in the source WOLF form. */
public final class WerewolfWolfModel extends EntityModel<AvatarRenderState> {
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
        CubeDeformation deformation = CubeDeformation.NONE;
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, deformation)
                        .texOffs(16, 14).addBox(-3.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, deformation)
                        .texOffs(16, 14).addBox(1.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, deformation)
                        .texOffs(0, 10).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 3.0F, 4.0F, deformation),
                PartPose.offset(0.0F, 13.5F, -7.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(18, 14)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, deformation),
                PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, Mth.HALF_PI, 0.0F, 0.0F));
        root.addOrReplaceChild("upper_body",
                CubeListBuilder.create().texOffs(21, 0)
                        .addBox(-4.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, deformation),
                PartPose.offsetAndRotation(0.0F, 14.0F, -3.0F, Mth.HALF_PI, 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(9, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation),
                PartPose.offsetAndRotation(0.0F, 12.0F, 8.0F, (float) Math.PI / 5.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float swing = Mth.cos(state.walkAnimationPos * 0.6662F)
                * 1.4F * state.walkAnimationSpeed;
        rightHindLeg.xRot = swing;
        leftHindLeg.xRot = -swing;
        rightFrontLeg.xRot = -swing;
        leftFrontLeg.xRot = swing;
        body.xRot = Mth.HALF_PI;
        upperBody.xRot = Mth.HALF_PI;
        tail.xRot = (float) Math.PI / 5.0F;
        tail.yRot = Mth.cos(state.ageInTicks * 0.3F) * 0.2F;
    }
}
