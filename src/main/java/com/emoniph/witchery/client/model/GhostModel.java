package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Shared ghost model used by Banshee, Spectre, and Poltergeist.
 * Ported directly from the original ModelSpectre (64×32 texture).
 * Parts: head, body, right_arm, left_arm, robe_upper, robe_lower.
 *
 * Arms-reaching variant (LAYER_SPECTRE) holds arms further forward.
 */
public class GhostModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_BANSHEE =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "banshee"), "main");
    public static final ModelLayerLocation LAYER_SPECTRE =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "spectre"), "main");
    public static final ModelLayerLocation LAYER_POLTERGEIST =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "poltergeist"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    /** Whether this instance uses the reaching-arms pose (Spectre = true, Banshee/Poltergeist = false). */
    private final boolean reachingArms;

    public GhostModel(ModelPart root, boolean reachingArms) {
        super(root);
        this.head       = root.getChild("head");
        this.body       = root.getChild("body");
        this.rightArm   = root.getChild("right_arm");
        this.leftArm    = root.getChild("left_arm");
        this.reachingArms = reachingArms;
    }

    /** Convenience constructor (reachingArms = false). */
    public GhostModel(ModelPart root) {
        this(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head — texOffs(0, 16) matches banshee.png UV layout
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        // Flat face detail (mouth/jaw plate) — from original ModelSpectre texOffs(56,0)
        // texOffs(40,0): needs u=40..50 — within 64px; old (56,0) overflowed to u=66
        head.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(40, 0).addBox(0f, 0f, 0f, 4, 5, 1),
                PartPose.offset(-2f, -4f, -4.1f));

        // Opaque chin plate at the base of the head (y=0) to cover transparent lower-head pixels.
        // Uses body UV region (texOffs 16,0) which is opaque in banshee.png.
        head.addOrReplaceChild("chin_plate",
                CubeListBuilder.create().texOffs(16, 0).addBox(-4, 0, -4, 8, 1, 8),
                PartPose.ZERO);

        // Torso
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 0).addBox(-4, 0, -2, 8, 10, 4),
                PartPose.offset(0, 0, 0));

        // Arms raised forward — initial rotation matches original -1.396263 rad (~-80°)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2, -2, 4, 12, 4),
                PartPose.offsetAndRotation(-5, 2, 0, -1.396263F, 0, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -2, 4, 12, 4),
                PartPose.offsetAndRotation(5, 2, 0, -1.396263F, 0, 0));

        // Flowing robe (two sections below the torso)
        root.addOrReplaceChild("robe_upper",
                CubeListBuilder.create().texOffs(38, 9).addBox(-4, 0, -2, 8, 6, 5),
                PartPose.offset(0, 10, 0));

        root.addOrReplaceChild("robe_lower",
                CubeListBuilder.create().texOffs(32, 20).addBox(-5, 0, -2, 10, 6, 6),
                PartPose.offset(0, 16, 0));

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

        // Arms: reaching forward for Spectre, loosely trailing for Banshee/Poltergeist
        float targetArmXRot = reachingArms ? -1.5F : -0.2F;
        float armBob = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot = targetArmXRot + armBob;
        leftArm.xRot  = targetArmXRot - armBob;

        // Gentle floating bob on the body
        body.y = Mth.sin(ageInTicks * 0.05F) * 0.5F;
    }
}
