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

public class GrassperModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "grassper"), "main");

    public GrassperModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("top_stalk",
                CubeListBuilder.create().texOffs(0, 4).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f),
                PartPose.offsetAndRotation(2.0f, 21.0f, 0.0f, 0.0f, 0.0f, -(float) Math.PI / 6.0f));
        root.addOrReplaceChild("right_leaf",
                CubeListBuilder.create().texOffs(0, 8).addBox(0.0f, 0.0f, -4.0f, 8.0f, 0.0f, 8.0f),
                PartPose.offsetAndRotation(0.0f, 24.0f, 0.0f, 0.0f, (float) Math.PI, (float) Math.PI / 6.0f));
        root.addOrReplaceChild("front_leaf",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 0.0f, -8.0f, 8.0f, 0.0f, 8.0f),
                PartPose.offsetAndRotation(0.0f, 24.0f, 0.0f, -(float) Math.PI / 6.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("back_leaf",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 0.0f, -8.0f, 8.0f, 0.0f, 8.0f),
                PartPose.offsetAndRotation(0.0f, 24.0f, 0.0f, -(float) Math.PI / 6.0f, -(float) Math.PI, 0.0f));
        root.addOrReplaceChild("left_leaf",
                CubeListBuilder.create().texOffs(0, 8).addBox(0.0f, 0.0f, -4.0f, 8.0f, 0.0f, 8.0f),
                PartPose.offsetAndRotation(0.0f, 24.0f, 0.0f, 0.0f, 0.0f, -(float) Math.PI / 6.0f));
        root.addOrReplaceChild("back_right_petal",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.0f, 0.0f, 1.0f, 2.0f, 1.0f),
                PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f, -(float) Math.PI / 6.0f, 0.0f, -(float) Math.PI / 4.0f));
        root.addOrReplaceChild("bottom_stalk",
                CubeListBuilder.create().texOffs(0, 10).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f),
                PartPose.offsetAndRotation(0.0f, 24.0f, 0.0f, 0.0f, 0.0f, (float) Math.PI / 6.0f));
        root.addOrReplaceChild("front_right_petal",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.0f, -1.0f, 1.0f, 2.0f, 1.0f),
                PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f, (float) Math.PI / 6.0f, 0.0f, -(float) Math.PI / 4.0f));
        root.addOrReplaceChild("back_left_petal",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -2.0f, 0.0f, 1.0f, 2.0f, 1.0f),
                PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f, -0.3490659f, 0.0f, (float) Math.PI / 12.0f));
        root.addOrReplaceChild("front_left_petal",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -2.0f, -1.0f, 1.0f, 2.0f, 1.0f),
                PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f, 0.3490659f, 0.0f, (float) Math.PI / 12.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
