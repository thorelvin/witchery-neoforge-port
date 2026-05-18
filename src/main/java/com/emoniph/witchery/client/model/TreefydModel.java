package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.TreefydEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Treefyd — log-shaped body, root legs, branch arms. 64×32 texture. */
public class TreefydModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "treefyd"), "main");
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public TreefydModel(ModelPart root) {
        super(root);
        this.body     = root.getChild("body");
        this.head     = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 12).addBox(-4, 0, -3, 8, 12, 6),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(28, 12).addBox(-2, -1, -1, 3, 10, 3),
                PartPose.offset(-6, 1, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(28, 12).mirror().addBox(-1, -1, -1, 3, 10, 3),
                PartPose.offset(6, 1, 0));

        // Legs placed at (40, 0) — safely inside the 64×32 UV space
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(40, 0).addBox(-2, 0, -2, 4, 9, 4),
                PartPose.offset(-2.5f, 12, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-2, 0, -2, 4, 9, 4),
                PartPose.offset(2.5f, 12, 0));

        return LayerDefinition.create(mesh, 64, 32);
    }
    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.xRot =  swing;
        leftLeg.xRot  = -swing;
        rightArm.xRot = -swing;
        leftArm.xRot  =  swing;
    }
}
