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

public class BeartrapModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "beartrap"), "main");

    private static final float CLOSED_JAW_ROTATION = 1.309F;

    private final ModelPart northSide;
    private final ModelPart southSide;

    public BeartrapModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        ModelPart body = root.getChild("body");
        this.northSide = body.getChild("north_side");
        this.southSide = body.getChild("south_side");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(11, 4).addBox(3.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(11, 4).addBox(-5.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(1, 19).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 1.0F, 4.0F)
                .texOffs(4, 5).addBox(-3.0F, -1.0F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition northSide = body.addOrReplaceChild("north_side", CubeListBuilder.create()
                .texOffs(4, 13).addBox(-4.5F, -1.0F, -7.0F, 9.0F, 1.0F, 0.0F)
                .texOffs(1, 6).addBox(-4.5F, 0.0F, -7.0F, 9.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));
        northSide.addOrReplaceChild("north_cube_1",
                CubeListBuilder.create().texOffs(1, 6).addBox(-0.5F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 0.5F, -5.5F, 0.0F, -1.5708F, 0.0F));
        northSide.addOrReplaceChild("north_cube_2",
                CubeListBuilder.create().texOffs(1, 6).addBox(-0.5F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 0.5F, -5.5F, 0.0F, -1.5708F, 0.0F));

        PartDefinition southSide = body.addOrReplaceChild("south_side", CubeListBuilder.create()
                .texOffs(4, 13).addBox(-4.5F, -1.0F, 7.0F, 9.0F, 1.0F, 0.0F)
                .texOffs(1, 6).addBox(-4.5F, 0.0F, 6.0F, 9.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));
        southSide.addOrReplaceChild("south_cube_1",
                CubeListBuilder.create().texOffs(3, 6).addBox(0.5F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 0.5F, 0.5F, 0.0F, -1.5708F, 0.0F));
        southSide.addOrReplaceChild("south_cube_2",
                CubeListBuilder.create().texOffs(4, 6).addBox(1.5F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 0.5F, -0.5F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public void setClosed(boolean closed) {
        northSide.xRot = closed ? -CLOSED_JAW_ROTATION : 0.0F;
        southSide.xRot = closed ? CLOSED_JAW_ROTATION : 0.0F;
    }
}
