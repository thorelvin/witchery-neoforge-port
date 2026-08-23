package com.emoniph.witchery.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Exact 1.7.10 {@code ModelBabaYaga} geometry on its inherited villager rig. */
public class BabaYagaModel extends EntityModel<BabaYagaModel.BabaYagaRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "baba_yaga"), "main");

    private static final float PESTLE_X_ROT = -1.152537F;
    private static final float PESTLE_Y_ROT = -2.305074F;
    private static final float PESTLE_Z_ROT = 1.839205F;

    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart arms;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart pestle;

    public BabaYagaModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.nose = this.head.getChild("nose");
        this.arms = root.getChild("arms");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.pestle = root.getChild("pestle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8),
                PartPose.ZERO);
        PartDefinition nose = head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        nose.addOrReplaceChild("nose_tip",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, 3.0F, -6.75F, 1, 1, 1, new CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, -2.0F, 0.0F));

        PartDefinition hatBrim = head.addOrReplaceChild("hat_brim",
                CubeListBuilder.create().texOffs(0, 98)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 2, 14),
                PartPose.offset(-7.0F, -10.03125F, -7.0F));
        PartDefinition hatTier1 = hatBrim.addOrReplaceChild("hat_tier1",
                CubeListBuilder.create().texOffs(0, 76)
                        .addBox(0.0F, 0.0F, 0.0F, 7, 4, 7),
                PartPose.offsetAndRotation(3.75F, -4.0F, 4.0F,
                        -0.05235988F, 0.0F, 0.02617994F));
        PartDefinition hatTier2 = hatTier1.addOrReplaceChild("hat_tier2",
                CubeListBuilder.create().texOffs(0, 87)
                        .addBox(0.0F, 0.0F, 0.0F, 4, 4, 4),
                PartPose.offsetAndRotation(1.75F, -4.0F, 2.0F,
                        -0.10471976F, 0.0F, 0.05235988F));
        hatTier2.addOrReplaceChild("hat_tip",
                CubeListBuilder.create().texOffs(0, 95)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 2, 1, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(1.75F, -2.0F, 2.0F,
                        (float) (-Math.PI / 15.0), 0.0F, 0.10471976F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6)
                        .texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6,
                                new CubeDeformation(0.5F)),
                PartPose.ZERO);
        root.addOrReplaceChild("arms",
                CubeListBuilder.create()
                        .texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4)
                        .texOffs(44, 22).addBox(4.0F, -2.0F, -2.0F, 4, 8, 4)
                        .texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4),
                PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 22)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 22).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("mortar",
                CubeListBuilder.create().mirror()
                        .texOffs(80, 88).addBox(1.0F, 7.0F, 2.0F, 12, 7, 12)
                        .texOffs(72, 107).addBox(0.0F, 0.0F, 1.0F, 14, 7, 14),
                PartPose.offset(-7.0F, 10.0F, -8.0F));
        root.addOrReplaceChild("pestle",
                CubeListBuilder.create().mirror()
                        .texOffs(124, 0).addBox(-1.0F, -7.0F, 0.0F, 1, 12, 1)
                        .texOffs(116, 13).addBox(-2.0F, 5.0F, -1.0F, 3, 12, 3),
                PartPose.offsetAndRotation(-3.0F, 6.0F, -4.0F,
                        PESTLE_X_ROT, PESTLE_Y_ROT, PESTLE_Z_ROT));
        root.addOrReplaceChild("cloak",
                CubeListBuilder.create().texOffs(94, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 8, 10, 0),
                PartPose.offsetAndRotation(-3.5F, -0.5F, 3.5F, 0.1F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(BabaYagaRenderState state) {
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        arms.x = 0.0F;
        arms.y = 3.0F;
        arms.z = -1.0F;
        arms.xRot = -0.75F;
        arms.yRot = 0.0F;
        arms.zRot = 0.0F;

        rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
                * 1.4F * state.walkAnimationSpeed * 0.5F;
        leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI)
                * 1.4F * state.walkAnimationSpeed * 0.5F;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;

        float frequency = 0.01F * (state.entityId % 10);
        float noseX = Mth.sin(state.entityTick * frequency) * 4.5F * Mth.DEG_TO_RAD;
        float noseZ = Mth.cos(state.entityTick * frequency) * 2.5F * Mth.DEG_TO_RAD;
        nose.x = 0.0F;
        nose.y = state.isHoldingItem ? 1.0F : -2.0F;
        nose.z = state.isHoldingItem ? -1.5F : 0.0F;
        nose.xRot = state.isHoldingItem ? -0.9F : noseX;
        nose.yRot = 0.0F;
        nose.zRot = noseZ;

        pestle.xRot = PESTLE_X_ROT + noseX;
        pestle.yRot = PESTLE_Y_ROT;
        pestle.zRot = PESTLE_Z_ROT + noseZ;
    }

    /** The source held-item renderer applies only the nose's own transform, not the head parent. */
    public void translateToSourceHeldItemAnchor(PoseStack poseStack) {
        nose.translateAndRotate(poseStack);
    }

    public static class BabaYagaRenderState extends HoldingEntityRenderState {
        public int entityId;
        public float entityTick;
        public boolean isHoldingItem;
    }
}
