package com.emoniph.witchery.client.model;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
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

/** Exact source {@code ModelOwl}, including landed, flying, sitting, and child poses. */
public class OwlModel extends EntityModel<OwlModel.OwlRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "owl"), "main");
    public static final ModelLayerLocation BABY_LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "owl"), "baby");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public OwlModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = body.getChild("right_leg");
        this.leftLeg = body.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror()
                        .texOffs(37, 0).addBox(-5.0F, -7.0F, -1.0F, 1, 3, 2)
                        .texOffs(37, 0).addBox(4.0F, -7.0F, -1.0F, 1, 3, 2)
                        .texOffs(30, 0).addBox(-1.0F, -3.0F, -4.0F, 2, 3, 1)
                        .texOffs(0, 0).addBox(-4.0F, -6.0F, -3.0F, 8, 6, 6),
                PartPose.offset(0.0F, 15.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).mirror()
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 8, 4),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        body.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-1.0F, 0.0F, -2.0F, 2, 1, 4),
                PartPose.offset(2.0F, 8.0F, -1.0F));
        body.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-1.0F, 0.0F, -2.0F, 2, 1, 4),
                PartPose.offset(-2.0F, 8.0F, -1.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1.0F, -1.0F, -2.0F, 1, 8, 4),
                PartPose.offset(-3.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(0.0F, -1.0F, -2.0F, 1, 8, 4),
                PartPose.offset(3.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createBabyLayer() {
        return createBodyLayer().apply(new BabyModelTransform(
                true, 11.0F, 0.0F, 2.0F, 2.0F, 24.0F, Set.of("head")));
    }

    @Override
    public void setupAnim(OwlRenderState state) {
        resetPose();
        if (state.landed) {
            body.xRot = 0.0F;
            rightArm.zRot = 0.0F;
            leftArm.zRot = 0.0F;
            rightLeg.xRot = 0.0F;
            leftLeg.xRot = 0.0F;
        } else {
            float flightTilt = Mth.PI / 4.0F + Mth.cos(state.ageInTicks * 0.1F) * 0.15F;
            body.xRot = flightTilt;
            body.yRot = 0.0F;
            rightLeg.xRot = flightTilt;
            leftLeg.xRot = flightTilt;
            rightArm.zRot = Mth.cos(state.ageInTicks * 0.5F) * Mth.PI * 0.4F + 1.4F;
            leftArm.zRot = -rightArm.zRot;
        }

        rightLeg.yRot = state.sitting ? 0.5F : 0.1F;
        leftLeg.yRot = -rightLeg.yRot;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
    }

    public static class OwlRenderState extends LivingEntityRenderState {
        public boolean landed;
        public boolean sitting;
        public int featherColor;
        public int tintColor = -1;
    }
}
