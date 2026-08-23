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

/**
 * Mindrake model — ported from ModelMindrake (1.7.10-0.24.1). A little turnip-rooted creature:
 * a flat leaf fan on top, a two-part tuber body, and two stub legs. 64×32 sheet.
 */
public class MindrakeModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "mindrake"), "main");

    private final ModelPart legLeft;
    private final ModelPart legRight;

    public MindrakeModel(ModelPart root) {
        super(root);
        this.legLeft  = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("leaves",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, 0.0F, 6, 6, 0),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("body_top",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.5F, 0.0F, -1.5F, 3, 1, 3),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("body_bottom",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 0.0F, -2.0F, 4, 3, 4),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(0, 20).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 1),
                PartPose.offset(1.0F, 23.0F, 0.0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(0, 20).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 1),
                PartPose.offset(-1.0F, 23.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        this.legLeft.xRot = 0.0F;
        this.legRight.xRot = 0.0F;
    }
}
