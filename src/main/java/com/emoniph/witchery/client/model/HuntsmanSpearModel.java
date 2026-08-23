package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class HuntsmanSpearModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "huntsman_spear_held"), "main");

    public HuntsmanSpearModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("shaft", mirrored(0, 0)
                        .addBox(-0.5F, -18.0F, -0.5F, 1, 32, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("head_front", mirrored(6, 3)
                        .addBox(-1.5F, -6.0F, 0.0F, 3, 6, 0),
                PartPose.offset(0.0F, -17.0F, 0.0F));
        root.addOrReplaceChild("head_side", mirrored(6, 0)
                        .addBox(0.0F, -6.0F, -1.5F, 0, 6, 3),
                PartPose.offset(0.0F, -17.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder mirrored(int u, int v) {
        return CubeListBuilder.create().mirror().texOffs(u, v);
    }
}
