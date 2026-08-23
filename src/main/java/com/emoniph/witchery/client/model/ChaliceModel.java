package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class ChaliceModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "chalice"), "main");

    private final ModelPart chalice;
    private final ModelPart liquid;

    public ChaliceModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        chalice = root.getChild("chalice");
        liquid = root.getChild("liquid");
    }

    public ModelPart chalice() {
        return chalice;
    }

    public ModelPart liquid() {
        return liquid;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder cup = CubeListBuilder.create().mirror()
                .texOffs(0, -5).addBox(4, -6, -1, 0, 4, 5)
                .texOffs(0, -5).addBox(-1, -6, -1, 0, 4, 5)
                .texOffs(0, 0).addBox(-1, -6, 4, 5, 4, 0)
                .texOffs(0, 0).addBox(-1, -6, -1, 5, 4, 0)
                .texOffs(-5, 4).addBox(-1, -2, -1, 5, 0, 5)
                .texOffs(4, 10).addBox(1, -2, 1, 1, 2, 1)
                .texOffs(0, 13).addBox(0, 0, 0, 3, 1, 3);
        root.addOrReplaceChild("chalice", cup,
                LegacyModelTransforms.render(-1, 23, -1, 0, 0, 0));
        root.addOrReplaceChild("liquid", CubeListBuilder.create().texOffs(-4, 18)
                        .addBox(0, 0, 0, 5, 0, 5),
                LegacyModelTransforms.render(-2, 19, -2, 0, 0, 0));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
