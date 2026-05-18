package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.DemonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Demon — imposing winged infernal biped. 128×32 texture.
 * UV layout and all rotation points ported 1:1 from original Witchery ModelDemon.
 * All root part offsets match original setRotationPoint values.
 * Head face details (horns, tusks, snout, lip) are inline addBox on the head part —
 * no children needed, matching the absolute position of the originals.
 */
public class DemonEntityModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "demon"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart wingRight;
    private final ModelPart wingLeft;

    public DemonEntityModel(ModelPart root) {
        super(root);
        this.head      = root.getChild("head");
        this.body      = root.getChild("body");
        this.rightArm  = root.getChild("right_arm");
        this.leftArm   = root.getChild("left_arm");
        this.rightLeg  = root.getChild("right_leg");
        this.leftLeg   = root.getChild("left_leg");
        this.wingRight = root.getChild("wing_right");
        this.wingLeft  = root.getChild("wing_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Head — rotation point (0,-9,0) matching original ─────────────────
        // All face detail addBox positions are relative to this rotation point,
        // replicating the original ModelDemon addBox calls exactly.
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        // Head face
                        .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                        // Horns: above crown (y<-8 from pivot = above head top)
                        .texOffs(0, 16).addBox(4, -12, -0.5f, 1, 8, 1)    // left horn
                        .texOffs(0, 16).addBox(-5, -12, -0.5f, 1, 8, 1)   // right horn
                        // Snout (mid-lower face)
                        .texOffs(20, 16).addBox(-1, -6, -6, 2, 3, 2)
                        // Bottom lip
                        .texOffs(8, 16).addBox(-2, -2, -6, 4, 1, 2)
                        // Tusks
                        .texOffs(4, 16).addBox(1, -4, -5, 1, 2, 1)        // left tusk
                        .texOffs(4, 16).addBox(-2, -4, -5, 1, 2, 1),      // right tusk
                PartPose.offset(0, -9, 0));

        // ── Body ─────────────────────────────────────────────────────────────
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(64, 0).addBox(-4, 0, -3, 8, 14, 6),
                PartPose.offset(0, -9, 0));

        // ── Arms ─────────────────────────────────────────────────────────────
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(48, 0).addBox(-3, -2, -2, 4, 20, 4),
                PartPose.offset(-5, -7, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1, -2, -2, 4, 20, 4),
                PartPose.offset(5, -7, 0));

        // ── Legs ─────────────────────────────────────────────────────────────
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(32, 0).addBox(-2, 0, -2, 4, 20, 4),
                PartPose.offset(-2, 4, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-2, 0, -2, 4, 20, 4),
                PartPose.offset(2, 4, 0));

        // ── Wings — attached high on back (y=-8), original rotations preserved
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(93, 0).addBox(0, 0, 0, 14, 21, 1),
                PartPose.offsetAndRotation(1, -8, 3,
                        0.3047198f, -0.6698132f, -0.6283185f));

        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(93, 0).mirror().addBox(-14, 0, 0, 14, 21, 1),
                PartPose.offsetAndRotation(-1, -8, 3,
                        -0.3047198f, (float) Math.PI + 0.6698132f, 0.6283185f));

        return LayerDefinition.create(mesh, 128, 32);
    }
    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = headPitch  * Mth.DEG_TO_RAD;

        float swing = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.xRot =  swing;
        leftLeg.xRot  = -swing;
        rightArm.xRot = -swing;
        leftArm.xRot  =  swing;

        float flapBase = Mth.sin(ageInTicks * 0.08f) * 0.08f;
        wingRight.zRot = -0.6283185f + flapBase;
        wingLeft.zRot  =  0.6283185f - flapBase;
    }
}
