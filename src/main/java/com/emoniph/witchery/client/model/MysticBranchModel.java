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

public class MysticBranchModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "mystic_branch_held"), "main");

    public MysticBranchModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("branch_1", mirrored(12, 0)
                        .addBox(0.0F, -4.0F, 0.0F, 1, 4, 1),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.1858931F, 0.0F, 0.2230717F));
        root.addOrReplaceChild("branch_2", mirrored(20, 0)
                        .addBox(-0.5F, -5.2F, -1.2F, 1, 5, 1),
                PartPose.offsetAndRotation(1.0F, -9.0F, 1.0F, -0.2230717F, 0.0F, -0.4089647F));
        root.addOrReplaceChild("branch_3", mirrored(4, 0)
                        .addBox(-1.0F, -6.0F, 0.0F, 1, 6, 1),
                PartPose.offsetAndRotation(1.0F, -4.0F, 0.0F, 0.2230717F, -0.0371786F, 0.4089647F));
        root.addOrReplaceChild("branch_4", mirrored(8, 0)
                        .addBox(-1.0F, -3.5F, -0.5F, 1, 4, 1),
                PartPose.offsetAndRotation(1.0F, -4.9F, 1.0F, -0.5948578F, 0.0F, -0.4089647F));
        root.addOrReplaceChild("branch_5", mirrored(16, 0)
                        .addBox(-0.2F, -4.8F, 0.4F, 1, 5, 1),
                PartPose.offsetAndRotation(1.0F, -12.0F, -1.0F, -0.3717861F, 0.0F, 0.0F));
        root.addOrReplaceChild("branch_6", mirrored(0, 0)
                        .addBox(0.0F, -8.0F, 0.0F, 1, 8, 1),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

    private static CubeListBuilder mirrored(int u, int v) {
        return CubeListBuilder.create().mirror().texOffs(u, v);
    }
}
