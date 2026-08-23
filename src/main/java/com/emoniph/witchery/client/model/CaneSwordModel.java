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

public class CaneSwordModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "cane_sword_held"), "main");

    private final ModelPart sheath;
    private final ModelPart ball;
    private final ModelPart blade1;
    private final ModelPart blade2;
    private final ModelPart blade3;

    public CaneSwordModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.sheath = root.getChild("sheath");
        this.ball = root.getChild("ball");
        this.blade1 = root.getChild("blade_1");
        this.blade2 = root.getChild("blade_2");
        this.blade3 = root.getChild("blade_3");
    }

    public ModelPart sheath() {
        return sheath;
    }

    public ModelPart ball() {
        return ball;
    }

    public ModelPart blade1() {
        return blade1;
    }

    public ModelPart blade2() {
        return blade2;
    }

    public ModelPart blade3() {
        return blade3;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("blade_3", CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-1.0F, -17.0F, -1.0F, 2, 2, 2),
                PartPose.offset(-5.8F, 11.0F, -1.6F));
        root.addOrReplaceChild("blade_2", CubeListBuilder.create().texOffs(24, 5)
                        .addBox(-1.0F, -15.0F, -1.0F, 2, 4, 2),
                PartPose.offset(-5.8F, 11.0F, -1.6F));
        root.addOrReplaceChild("ball", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.5F, -1.0F, -1.5F, 3, 2, 3),
                PartPose.offset(-5.8F, 10.0F, -1.6F));
        root.addOrReplaceChild("blade_1", CubeListBuilder.create().texOffs(24, 12)
                        .addBox(-1.0F, -8.0F, -1.0F, 2, 9, 2),
                PartPose.offset(-5.8F, 8.0F, -1.6F));
        root.addOrReplaceChild("sheath", CubeListBuilder.create().texOffs(0, 6)
                        .addBox(-1.0F, 0.0F, -1.0F, 2, 13, 2),
                PartPose.offset(-5.8F, 11.0F, -1.6F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
