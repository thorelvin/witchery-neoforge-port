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

public class WolfStatueModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "statue_wolf"), "main");

    public WolfStatueModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror().texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F),
                PartPose.offset(-0.5F, 13.5F, -7.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(18, 14)
                        .addBox(-4.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                PartPose.offsetAndRotation(0.5F, 14.0F, 2.0F, 1.570796F, 0.0F, 0.0F));
        root.addOrReplaceChild("mane",
                CubeListBuilder.create().mirror().texOffs(21, 0)
                        .addBox(-4.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(-0.5F, 14.0F, -3.0F, 1.570796F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-2.0F, 16.0F, 7.0F));
        root.addOrReplaceChild("rear_right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(1.0F, 16.0F, 7.0F));
        root.addOrReplaceChild("front_left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-2.0F, 16.0F, -4.0F));
        root.addOrReplaceChild("front_right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(1.0F, 16.0F, -4.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().mirror().texOffs(9, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-0.5F, 12.0F, 8.0F, 1.130069F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_ear",
                CubeListBuilder.create().mirror().texOffs(16, 14)
                        .addBox(-3.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-0.5F, 13.5F, -7.0F));
        root.addOrReplaceChild("right_ear",
                CubeListBuilder.create().mirror().texOffs(16, 14)
                        .addBox(1.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-0.5F, 13.5F, -7.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().mirror().texOffs(0, 10)
                        .addBox(-2.0F, 0.0F, -5.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 13.5F, -7.0F));
        root.addOrReplaceChild("pedestal",
                CubeListBuilder.create().mirror().texOffs(22, 18)
                        .addBox(0.0F, 0.0F, 0.0F, 8.0F, 1.0F, 13.0F),
                PartPose.offset(-4.5F, 23.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }
}
