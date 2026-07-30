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

public class MandrakeStatueModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "statue_mandrake"), "main");

    public MandrakeStatueModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        head.addOrReplaceChild("face",
                CubeListBuilder.create().mirror().texOffs(0, 8)
                        .addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("leaves",
                CubeListBuilder.create().mirror().texOffs(0, 0)
                        .addBox(-4.0F, -12.0F, 0.0F, 8.0F, 8.0F, 0.0F),
                PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        body.addOrReplaceChild("chest",
                CubeListBuilder.create().mirror().texOffs(21, 0)
                        .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 2.0F, 5.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("belly",
                CubeListBuilder.create().mirror().texOffs(17, 7)
                        .addBox(-3.5F, 2.0F, -3.5F, 7.0F, 3.0F, 7.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(37, 0)
                        .addBox(-1.0F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 17.0F, 0.0F, 0.0F, 0.0F, 1.047198F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(37, 0)
                        .addBox(0.0F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 17.0F, 0.0F, 0.0F, 0.0F, -1.047198F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().mirror().texOffs(27, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-1.0F, 21.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(27, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(1.0F, 21.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }
}
