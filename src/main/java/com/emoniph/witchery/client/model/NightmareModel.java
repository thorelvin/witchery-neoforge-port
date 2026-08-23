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
 * Nightmare model — ported from ModelNightmare (64×64).
 * Humanoid wraith with 7 animated trailing tendrils below the torso.
 */
public class NightmareModel extends EntityModel<NightmareModel.NightmareRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "nightmare"), "main");
    private static final int[] LEG_SWAY_MODULI = {12, 12, 13, 10, 12, 10, 10};
    private static final float LEG_BASE_Z_ROT = 1.839205f;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart[] legs = new ModelPart[7];

    public static class NightmareRenderState extends LivingEntityRenderState {
        public float attackTimer;
        public int entityId;
        public int entityTick;
        public boolean defended;
    }

    public NightmareModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        for (int i = 0; i < 7; i++) legs[i] = root.getChild("leg" + (i + 1));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -8f, -3f, 8, 8, 6),
                PartPose.offset(0f, -6f, 0f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4f, 0f, -2f, 8, 12, 4),
                PartPose.offset(0f, -6f, 0f));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1f, -2f, -1f, 2, 16, 2),
                PartPose.offset(-5f, -4f, 0f));
        rightArm.addOrReplaceChild("rf_little",
                CubeListBuilder.create().texOffs(0, 46).addBox(-1f, 0f, -1f, 1, 6, 1),
                PartPose.offsetAndRotation(0f, 12f, 0f, -0.148353f, 0f, 0.1134464f));
        rightArm.addOrReplaceChild("rf_index",
                CubeListBuilder.create().texOffs(4, 46).addBox(0f, 0f, -1f, 1, 6, 1),
                PartPose.offsetAndRotation(0f, 12f, 0f, -0.148353f, 0f, -0.1134464f));
        rightArm.addOrReplaceChild("rf_thumb",
                CubeListBuilder.create().texOffs(8, 46).addBox(-0.5f, 0f, 0f, 1, 6, 1),
                PartPose.offsetAndRotation(0f, 12f, 0f, 0.1396263f, 0f, 0f));

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1f, -2f, -1f, 2, 16, 2),
                PartPose.offset(5f, -4f, 0f));
        leftArm.addOrReplaceChild("lf_little",
                CubeListBuilder.create().texOffs(8, 53).addBox(0f, 0f, -1f, 1, 6, 1),
                PartPose.offsetAndRotation(0f, 12f, 0f, -0.148353f, 0f, -0.1134464f));
        leftArm.addOrReplaceChild("lf_index",
                CubeListBuilder.create().texOffs(4, 53).addBox(-1f, 0f, -1f, 1, 6, 1),
                PartPose.offsetAndRotation(0f, 12f, 0f, -0.148353f, 0f, 0.1134464f));
        leftArm.addOrReplaceChild("lf_thumb",
                CubeListBuilder.create().texOffs(0, 53).addBox(-0.5f, 0f, 0f, 1, 6, 1),
                PartPose.offsetAndRotation(0f, 12f, 0f, 0.1396263f, 0f, 0f));

        root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(0,  16).addBox(0f, 0f, 0f, 1,  3, 1), PartPose.offset(-2f, 6f, -1f));
        root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(0,  16).addBox(0f, 0f, 0f, 1, 14, 1), PartPose.offset( 2f, 6f,  0f));
        root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(12, 16).addBox(0f, 0f, 0f, 1, 11, 1), PartPose.offset( 1f, 6f, -1f));
        root.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(8,  16).addBox(0f, 0f, 0f, 1, 14, 1), PartPose.offset(-1f, 6f, -1f));
        root.addOrReplaceChild("leg5", CubeListBuilder.create().texOffs(4,  16).addBox(0f, 0f, 0f, 1, 12, 1), PartPose.offset(-2f, 6f,  0f));
        root.addOrReplaceChild("leg6", CubeListBuilder.create().texOffs(8,  16).addBox(0f, 0f, 0f, 1, 17, 1), PartPose.offset( 3f, 6f, -1f));
        root.addOrReplaceChild("leg7", CubeListBuilder.create().texOffs(12, 16).addBox(0f, 0f, 0f, 1, 16, 1), PartPose.offset( 0f, 6f,  0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NightmareRenderState state) {
        float entityTick = state.entityTick;
        float headPitch = state.xRot;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = headPitch  * Mth.DEG_TO_RAD;

        float atk = state.attackTimer;
        if (atk > 0.0f) {
            float t = triangleWave(atk, 15f);
            rightArm.xRot = -1.5f + 0.8f * t;
            leftArm.xRot  = -1.5f + 0.8f * t;
            rightArm.zRot =  1.5f - 1.5f * t;
            leftArm.zRot  = -1.5f + 1.5f * t;
        } else {
            rightArm.xRot = -0.1f; leftArm.xRot = -0.1f;
            rightArm.zRot =  0f;   leftArm.zRot = 0f;
        }

        int id = state.entityId;
        for (int i = 0; i < 7; i++) {
            float f = 0.01f * (float)(id % LEG_SWAY_MODULI[i]);
            legs[i].xRot = -1.8f + Mth.sin(entityTick * f) * 4.5f * Mth.DEG_TO_RAD;
            legs[i].yRot = -1.7f;
            legs[i].zRot = LEG_BASE_Z_ROT + Mth.cos(entityTick * f) * 2.5f * Mth.DEG_TO_RAD;
        }
    }

    private static float triangleWave(float x, float period) {
        return (Math.abs(x % period - period * 0.5f) - period * 0.25f) / (period * 0.25f);
    }
}
