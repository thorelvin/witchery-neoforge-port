package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class EarmuffsModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "earmuffs"), "main");

    public EarmuffsModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("band_top",
                CubeListBuilder.create().texOffs(46, 38).mirror().addBox(-4.0F, -10.0F, -0.5F, 8.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(33, 32).mirror().addBox(-6.0F, -6.0F, -2.0F, 2.0F, 4.0F, 4.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(33, 32).mirror().addBox(4.0F, -6.0F, -2.0F, 2.0F, 4.0F, 4.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("band_left",
                CubeListBuilder.create().texOffs(46, 32).mirror().addBox(4.0F, -10.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("band_right",
                CubeListBuilder.create().texOffs(46, 32).mirror().addBox(-5.0F, -10.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
