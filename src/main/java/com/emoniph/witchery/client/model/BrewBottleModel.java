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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** The source 3D held model registered for the Brew Bag. */
public class BrewBottleModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "brew_bottle_held"), "main");

    private final List<ModelPart> parts;

    public BrewBottleModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.parts = List.of(root.getChild("bottle"), root.getChild("stopper"));
    }

    public Iterable<ModelPart> parts() {
        return parts;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("bottle", CubeListBuilder.create().mirror()
                        .texOffs(0, 14).addBox(-1.5F, -2.0F, -1.5F, 3, 2, 3)
                        .texOffs(2, 8).addBox(-1.0F, -2.5F, -1.0F, 2, 3, 2)
                        .texOffs(4, 4).addBox(-0.5F, -4.0F, -0.5F, 1, 2, 1),
                PartPose.ZERO);

        // ModelBrewBottle assigns mirror after addBox, so the stopper's baked winding is unmirrored.
        root.addOrReplaceChild("stopper", CubeListBuilder.create().texOffs(2, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 2, 1, 2),
                PartPose.offset(-1.0F, -4.5F, -1.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
