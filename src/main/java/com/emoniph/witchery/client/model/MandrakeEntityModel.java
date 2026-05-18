package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.MandrakeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Mandrake mob — tiny shrieking root-creature. 32×32 texture. */
public class MandrakeEntityModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "mandrake_mob"), "main");
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MandrakeEntityModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Bulbous head with screaming face
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6),
                PartPose.offset(0, 12, 0));

        // Root legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 18).addBox(-1, 0, -1, 2, 6, 2),
                PartPose.offset(-2, 12, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(8, 18).addBox(-1, 0, -1, 2, 6, 2),
                PartPose.offset(2, 12, 0));

        return LayerDefinition.create(mesh, 32, 32);
    }
    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        // Frantic running animation
        float swing = Mth.cos(limbSwing * 0.6662f) * 1.8f * limbSwingAmount;
        rightLeg.xRot =  swing;
        leftLeg.xRot  = -swing;
    }
}
