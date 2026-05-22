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
 * Mandrake mob — tiny screaming root creature. 64×32 texture.
 * Ported 1:1 from original ModelMandrake (witchery-1.7.2-0.19.0).
 */
public class MandrakeEntityModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "mandrake_mob"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MandrakeEntityModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm  = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg  = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head group: face cube + flat leaf crown billboard (depth=0)
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0, 16, 0));
        head.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(0, 8).addBox(-2, -4, -2, 4, 4, 4),
                PartPose.ZERO);
        head.addOrReplaceChild("leaves",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -12, -0.5f, 8, 8, 1),
                PartPose.ZERO);

        // Body group: chest mound + belly bulge
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create(),
                PartPose.offset(0, 16, 0));
        body.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(21, 0).addBox(-2.5f, 0, -2.5f, 5, 2, 5),
                PartPose.ZERO);
        body.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(17, 7).addBox(-3.5f, 2, -3.5f, 7, 3, 7),
                PartPose.ZERO);

        // Tiny twig arms angled outward at PI/3
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(37, 0).addBox(-1, 0, -0.5f, 1, 3, 1),
                PartPose.offsetAndRotation(-2, 17, 0, 0, 0, Mth.PI / 3));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(37, 0).addBox(0, 0, -0.5f, 1, 3, 1),
                PartPose.offsetAndRotation(2, 17, 0, 0, 0, -Mth.PI / 3));

        // Root legs — bottom at y=21+3=24 (floor reference)
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(27, 18).addBox(-1, 0, -1, 2, 3, 2),
                PartPose.offset(-1, 21, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(27, 18).addBox(-1, 0, -1, 2, 3, 2),
                PartPose.offset(1, 21, 0));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing       = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float swing = Mth.cos(limbSwing * 0.6662f) * 1.5f * limbSwingAmount;
        rightLeg.xRot =  swing;
        leftLeg.xRot  = -swing;
        // Arms twitch slightly while walking
        rightArm.zRot =  Mth.PI / 3 - swing * 0.3f;
        leftArm.zRot  = -Mth.PI / 3 + swing * 0.3f;
    }
}
