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
 * Demon — imposing winged infernal biped. 128×32 texture.
 * UV layout and all rotation points ported 1:1 from original Witchery ModelDemon.
 * All root part offsets match original setRotationPoint values.
 * Head face details (horns, tusks, snout, lip) are inline addBox on the head part —
 * no children needed, matching the absolute position of the originals.
 */
public class DemonEntityModel extends EntityModel<DemonEntityModel.DemonRenderState> {

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
    private static final float WING_X_ROT = 0.3047198F;
    private static final float WING_Y_ROT = -0.6698132F;
    private static final float WING_Z_ROT = -0.6283185F;

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
                CubeListBuilder.create().texOffs(48, 0).addBox(-1, -2, -2, 4, 20, 4),
                PartPose.offset(5, -7, 0));

        // ── Legs ─────────────────────────────────────────────────────────────
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(32, 0).addBox(-2, 0, -2, 4, 20, 4),
                PartPose.offset(-2, 4, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(32, 0).addBox(-2, 0, -2, 4, 20, 4),
                PartPose.offset(2, 4, 0));

        // ── Wings — attached high on back (y=-8), original rotations preserved
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(93, 0).addBox(0, 0, 0, 14, 21, 0),
                PartPose.offsetAndRotation(1, -8, 3,
                        WING_X_ROT, WING_Y_ROT, WING_Z_ROT));

        // wing_left uses same box as wing_right; the PI+0.67 yRot mirrors it to the left side
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(93, 0).addBox(0, 0, 0, 14, 21, 0),
                PartPose.offsetAndRotation(-1, -8, 3,
                        -WING_X_ROT, (float) Math.PI - WING_Y_ROT, -WING_Z_ROT));

        return LayerDefinition.create(mesh, 128, 32);
    }
    @Override
    public void setupAnim(DemonRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float gait = triangleWave(limbSwing, 13.0F);
        leftLeg.xRot = -1.5F * gait * limbSwingAmount;
        rightLeg.xRot = 1.5F * gait * limbSwingAmount;
        leftLeg.yRot = 0.0F;
        rightLeg.yRot = 0.0F;
        leftArm.xRot = (-0.2F - 1.5F * gait) * limbSwingAmount;
        rightArm.xRot = state.attackTimer > 0.0F
                ? -2.0F + 1.5F * triangleWave(state.attackTimer, 10.0F)
                : (-0.2F + 1.5F * gait) * limbSwingAmount;
        wingRight.xRot = WING_X_ROT;
        wingRight.yRot = WING_Y_ROT;
        wingRight.zRot = WING_Z_ROT;
        wingLeft.xRot = -WING_X_ROT;
        wingLeft.yRot = (float) Math.PI - WING_Y_ROT;
        wingLeft.zRot = -WING_Z_ROT;
    }

    public static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }

    public static class DemonRenderState extends LivingEntityRenderState {
        public float attackTimer;
    }
}
