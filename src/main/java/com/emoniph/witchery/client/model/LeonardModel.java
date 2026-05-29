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
 * Shade of Leonard model — shadowy humanoid shade. 64×32 texture (leonard.png).
 */
public class LeonardModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "leonard"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public LeonardModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(16, 0).addBox(-4, 0, -2, 8, 10, 4),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-3, -2, -2, 4, 12, 4),
                PartPose.offsetAndRotation(-5, 2, 0, -0.4F, 0, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1, -2, -2, 4, 12, 4),
                PartPose.offsetAndRotation(5, 2, 0, -0.4F, 0, 0));

        root.addOrReplaceChild("robe_upper",
                CubeListBuilder.create().mirror().texOffs(38, 9).addBox(-4, 0, -2, 8, 6, 5),
                PartPose.offset(0, 10, 0));

        root.addOrReplaceChild("robe_lower",
                CubeListBuilder.create().mirror().texOffs(32, 20).addBox(-5, 0, -2, 10, 6, 6),
                PartPose.offset(0, 16, 0));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float ageInTicks = state.ageInTicks;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float armBob = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot = -0.4F + armBob;
        leftArm.xRot  = -0.4F - armBob;
    }
}
