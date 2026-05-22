package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Ent — massive tree-spirit with 8 root legs and large leaf canopy. 256×256 texture.
 * Ported 1:1 from original ModelEnt (witchery-1.7.2-0.19.0).
 */
public class EntModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "ent"), "main");

    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public EntModel(ModelPart root) {
        super(root);
        this.leftArm  = root.getChild("arm_left");
        this.rightArm = root.getChild("arm_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Branch arms — large vertical branches
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(82, 0).addBox(0, -22, -3, 6, 24, 6),
                PartPose.offset(8, -4, 0));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(82, 0).addBox(-6, -22, -3, 6, 24, 6),
                PartPose.offset(-8, -4, 0));

        // Massive trunk body — spans y=-46 to +2 relative to pivot at y=20
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 50).addBox(-8, -46, -8, 16, 48, 16),
                PartPose.offset(0, 20, 0));

        // Root legs — 8 thin horizontal roots extending outward from base (y=20)
        // Right side (4 legs): positive X pivot, extend further right
        root.addOrReplaceChild("leg8",
                CubeListBuilder.create().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, -1, 0, 0.5759587f, 0.1919862f));
        root.addOrReplaceChild("leg6",
                CubeListBuilder.create().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, 0, 0, 0.2792527f, 0.1919862f));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, 1, 0, -0.2792527f, 0.1919862f));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(18, 0).addBox(-3, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(4, 20, 2, 0, -0.5759587f, 0.1919862f));

        // Left side (4 legs): negative X pivot, extend further left
        root.addOrReplaceChild("leg7",
                CubeListBuilder.create().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, -1, 0, -0.5759587f, -0.1919862f));
        root.addOrReplaceChild("leg5",
                CubeListBuilder.create().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, 0, 0, -0.2792527f, -0.1919862f));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, 1, 0, 0.2792527f, -0.1919862f));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(18, 0).addBox(-13, -1, -1, 16, 2, 2),
                PartPose.offsetAndRotation(-4, 20, 2, 0, 0.5759587f, -0.1919862f));

        // Large leaf canopy — base layer (60×16×60 slab floating above trunk)
        root.addOrReplaceChild("leaves_base",
                CubeListBuilder.create().texOffs(0, 180).addBox(0, 0, 0, 60, 16, 60),
                PartPose.offset(-30, -42, -30));

        // Narrower upper leaves
        root.addOrReplaceChild("leaves_top",
                CubeListBuilder.create().texOffs(56, 130).addBox(0, 0, 0, 32, 16, 32),
                PartPose.offset(-16, -58, -16));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing       = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        // Gentle arm sway, no leg animation (root legs are static)
        rightArm.zRot = (-0.2f + 0.1f * Mth.cos(limbSwing * 0.6662f) * limbSwingAmount) - 0.1f;
        leftArm.zRot  = ( 0.2f - 0.1f * Mth.cos(limbSwing * 0.6662f) * limbSwingAmount) + 0.1f;
    }
}
