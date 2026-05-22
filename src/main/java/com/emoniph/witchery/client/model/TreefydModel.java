package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Treefyd - plant-spirit golem. 64x32 texture.
 * Ported from the original ModelTreefyd (witchery-1.7.2-0.19.0).
 * Zero-size boxes are intentional: the old model used planes for petals, tongue, and leaves.
 */
public class TreefydModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "treefyd"), "main");

    private final ModelPart head;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;

    public TreefydModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0, 3, 0));
        head.addOrReplaceChild("face",
                CubeListBuilder.create().mirror().texOffs(0, 24).addBox(-2, -4, -2, 4, 4, 4),
                PartPose.ZERO);
        head.addOrReplaceChild("petals",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-5, -7, 0, 10, 10, 0),
                PartPose.ZERO);
        head.addOrReplaceChild("tongue",
                CubeListBuilder.create().mirror().texOffs(25, 18).addBox(0, -3, -6, 0, 10, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 14).addBox(-1, 0, -1, 2, 16, 2),
                PartPose.offset(0, 3, 0));

        root.addOrReplaceChild("leaves_v",
                CubeListBuilder.create().texOffs(40, 6).addBox(0, 0, -6, 0, 14, 12),
                PartPose.offsetAndRotation(0, 6, 0, 0, Mth.PI / 4, 0));
        root.addOrReplaceChild("leaves_h",
                CubeListBuilder.create().texOffs(40, 0).addBox(-6, 0, 0, 12, 14, 0),
                PartPose.offsetAndRotation(0, 6, 0, 0, Mth.PI / 4, 0));

        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(15, 6).addBox(0, 0, 0, 6, 1, 6),
                PartPose.offset(-3, 19, -3));

        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(-2, 20, 4));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(2, 20, 4));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(-2, 20, -4));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(2, 20, -4));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float swing = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        leg1.xRot = swing;
        leg4.xRot = swing;
        leg2.xRot = -swing;
        leg3.xRot = -swing;
    }
}
