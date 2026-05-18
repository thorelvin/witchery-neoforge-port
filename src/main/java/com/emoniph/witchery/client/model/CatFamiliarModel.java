package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Cat familiar model — UV matches vanilla 1.7.10 ModelOcelot (blackcat.png is painted for it).
 * Body uses xRot=HALF_PI so the 4×16×6 box lies flat (quadruped pattern).
 * Texture: 64×32.
 */
public class CatFamiliarModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "familiar_cat"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;
    private final ModelPart tail1;

    public CatFamiliarModel(ModelPart root) {
        super(root);
        this.head         = root.getChild("head");
        this.body         = root.getChild("body");
        this.legFrontLeft  = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft   = root.getChild("leg_back_left");
        this.legBackRight  = root.getChild("leg_back_right");
        this.tail1         = root.getChild("tail1");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head — vanilla ocelot UV layout
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -2f, -3f, 5, 4, 5),
                PartPose.offset(0f, 12f, -5f));

        // Body — 4×16×6 with xRot=HALF_PI so it lies flat (16 units become length)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2f, 0f, -3f, 4, 16, 6),
                PartPose.offsetAndRotation(0f, 12f, 4f, Mth.HALF_PI, 0f, 0f));

        // Front legs
        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(8, 13).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offset(-1.2f, 16f, -5f));

        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(8, 13).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offset(1.2f, 16f, -5f));

        // Back legs
        root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(16, 13).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offset(-1.2f, 16f, 6f));

        root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(16, 13).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offset(1.2f, 16f, 6f));

        // Tail (two segments)
        PartDefinition tail1 = root.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 15).addBox(-0.5f, 0f, -0.5f, 1, 8, 1),
                PartPose.offsetAndRotation(-1f, 12f, 8f, -0.5236f, 0f, 0f)); // -30°

        tail1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(4, 15).addBox(-0.5f, 0f, -0.5f, 1, 5, 1),
                PartPose.offset(0f, 8f, 0f));

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
        legFrontLeft.xRot  =  swing;
        legFrontRight.xRot = -swing;
        legBackLeft.xRot   = -swing;
        legBackRight.xRot  =  swing;

        tail1.yRot = Mth.cos(ageInTicks * 0.067f) * 0.3f;
    }
}
