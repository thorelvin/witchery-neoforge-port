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
 * Source ModelGrotesque demon head used by both Grotesque effect routes.
 */
public final class GrotesqueModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "grotesque_head"), "main");

    private final ModelPart head;

    public GrotesqueModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(0, 16).addBox(4.0F, -12.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .texOffs(0, 16).addBox(-5.0F, -12.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .texOffs(4, 16).addBox(1.0F, -4.0F, -5.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(8, 16).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 1.0F, 2.0F)
                        .texOffs(20, 16).addBox(-1.0F, -6.0F, -6.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(4, 16).addBox(-2.0F, -4.0F, -5.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
    }
}
