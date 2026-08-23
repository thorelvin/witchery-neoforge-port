package com.emoniph.witchery.client.model;

import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class DemonHeartModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "demon_heart"), "main");

    private final List<ModelPart> staticParts;
    private final List<ModelPart> pulseParts;

    public DemonHeartModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        this.staticParts = List.of(
                root.getChild("tube1"),
                root.getChild("tube2"),
                root.getChild("tube3"),
                root.getChild("tube4"),
                root.getChild("tube5"),
                root.getChild("shape4"));
        this.pulseParts = List.of(
                root.getChild("big_tube1"),
                root.getChild("shape1"),
                root.getChild("shape2"),
                root.getChild("shape3"));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("shape1",
                CubeListBuilder.create().texOffs(14, 20).addBox(0.0F, 0.0F, 0.0F, 5.0F, 8.0F, 4.0F),
                PartPose.offset(-3.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("shape2",
                CubeListBuilder.create().texOffs(0, 7).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 6.0F),
                PartPose.offset(-4.0F, 15.0F, -1.0F));
        root.addOrReplaceChild("shape3",
                CubeListBuilder.create().texOffs(13, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offset(-5.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("shape4",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(-3.0F, 13.0F, 1.0F));
        root.addOrReplaceChild("big_tube1",
                CubeListBuilder.create().texOffs(3, 3).addBox(0.0F, 0.0F, 0.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, 15.0F, -1.0F, 0.0F, 0.3717861F, 0.2230717F));
        root.addOrReplaceChild("tube1",
                CubeListBuilder.create().texOffs(19, 11).addBox(0.0F, -3.0F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 14.0F, 1.0F, 0.4089647F, 0.6320364F, 0.0F));
        root.addOrReplaceChild("tube2",
                CubeListBuilder.create().texOffs(19, 11).addBox(0.0F, -3.0F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 14.0F, 1.0F, -0.2974289F, -0.2230717F, -0.3346075F));
        root.addOrReplaceChild("tube3",
                CubeListBuilder.create().texOffs(19, 11).addBox(0.0F, -3.0F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, 13.0F, -0.8F, -0.0743572F, 0.1487144F, -0.2602503F));
        root.addOrReplaceChild("tube4",
                CubeListBuilder.create().texOffs(19, 11).addBox(0.0F, -3.0F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.2602503F, 0.0F, 0.4089647F));
        root.addOrReplaceChild("tube5",
                CubeListBuilder.create().texOffs(19, 11).addBox(0.0F, -3.0F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 1.0F, -0.2602503F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public List<ModelPart> staticParts() {
        return staticParts;
    }

    public List<ModelPart> pulseParts() {
        return pulseParts;
    }
}
