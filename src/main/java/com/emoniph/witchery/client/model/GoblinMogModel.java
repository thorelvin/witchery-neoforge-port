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
 * Mog model — ported from ModelGoblinMog (1.7.10-0.24.1). 64×64 texture.
 * Distinctive features vs. base goblin:
 *   - Standard body height (8×12×4)
 *   - Slightly longer arms (4×14×4)
 *   - Tilted shoulder chest piece at texOffs(35,5)
 *   - Skirt/kilt at texOffs(14,34) hanging from the hip
 */
public class GoblinMogModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "goblin_mog"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart chest;
    private final ModelPart skirt;

    public GoblinMogModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
        this.chest    = root.getChild("chest");
        this.skirt    = root.getChild("skirt");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head — same goblin face structure
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                        .texOffs(0, 4).addBox(3, -4, -5, 1, 3, 1)
                        .texOffs(0, 4).addBox(-4, -4, -5, 1, 3, 1)
                        .texOffs(25, 0).addBox(-1, -3, -5, 2, 2, 1)
                        .texOffs(34, 0).addBox(-1, -1, -5, 2, 1, 1)
                        .texOffs(39, 0).addBox(4, -7, 0, 2, 3, 1)
                        .texOffs(39, 0).addBox(-6, -7, 0, 2, 3, 1),
                PartPose.offset(0, 0, 0));

        // Body — standard height (12 tall)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.offset(0, 0, 0));

        // Arms — slightly longer than base goblin (14 tall)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 14).addBox(-3, -2, -2, 4, 14, 4),
                PartPose.offset(-5, 2, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 14).mirror().addBox(-1, -2, -2, 4, 14, 4),
                PartPose.offset(5, 2, 0));

        // Legs
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-2, 12, 0));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(2, 12, 0));

        // Chest — small tilted shoulder piece at texOffs(35,5), rotated PI/4 forward
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(35, 5).addBox(-4, -2, -5, 8, 4, 4),
                PartPose.offsetAndRotation(0, 2, 0, (float)(Math.PI / 4), 0, 0));

        // Skirt/kilt at texOffs(14,34)
        root.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(14, 34).addBox(-4.5f, 0, -2.5f, 9, 11, 5),
                PartPose.offset(0, 12, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing  = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 2.0F * limbAmount * 0.5F;
        leftArm.xRot  = Mth.cos(limbSwing * 0.6662F)           * 2.0F * limbAmount * 0.5F;
        rightArm.zRot = 0;
        leftArm.zRot  = 0;

        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F)           * 1.4F * limbAmount;
        leftLeg.xRot  = Mth.cos(limbSwing * 0.6662F + Mth.PI)  * 1.4F * limbAmount;
    }
}
