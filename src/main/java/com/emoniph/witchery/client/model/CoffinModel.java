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
import net.minecraft.util.Mth;

public class CoffinModel extends Model.Simple {
    public static final ModelLayerLocation HEAD_LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "coffin"), "head");
    public static final ModelLayerLocation BOTTOM_LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "coffin"), "bottom");

    private final ModelPart lid;

    public CoffinModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.lid = root.getChild("base").getChild("lid");
    }

    public static LayerDefinition createHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(2, 22).addBox(-8.0F, 0.0F, 0.0F, 16.0F, 3.0F, 16.0F),
                PartPose.offset(0.0F, 21.0F, -8.0F));
        base.addOrReplaceChild("wall_right",
                CubeListBuilder.create().texOffs(1, 44).addBox(-7.0F, -10.0F, 0.0F, 1.0F, 10.0F, 14.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("wall_left",
                CubeListBuilder.create().texOffs(62, 44).addBox(6.0F, -10.0F, 0.0F, 1.0F, 10.0F, 14.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("wall_front",
                CubeListBuilder.create().texOffs(36, 45).addBox(-7.0F, -10.0F, 14.0F, 14.0F, 10.0F, 1.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("lid",
                CubeListBuilder.create().texOffs(4, 1).addBox(-1.0F, -1.0F, 0.0F, 16.0F, 2.0F, 16.0F),
                PartPose.offset(-7.0F, -11.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createBottomLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(2, 21).addBox(-8.0F, 0.0F, -16.0F, 16.0F, 3.0F, 16.0F),
                PartPose.offset(0.0F, 21.0F, 8.0F));
        base.addOrReplaceChild("wall_front",
                CubeListBuilder.create().texOffs(39, 45).addBox(-7.0F, -10.0F, -15.0F, 14.0F, 10.0F, 1.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("wall_left",
                CubeListBuilder.create().texOffs(62, 44).addBox(6.0F, -10.0F, -14.0F, 1.0F, 10.0F, 14.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("wall_right",
                CubeListBuilder.create().texOffs(1, 44).addBox(-7.0F, -10.0F, -14.0F, 1.0F, 10.0F, 14.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("lid",
                CubeListBuilder.create().texOffs(4, 0).addBox(-1.0F, -1.0F, 0.0F, 16.0F, 2.0F, 16.0F),
                PartPose.offset(-7.0F, -11.0F, -16.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public void setOpen(float openAmount) {
        lid.zRot = -(1.0F - (1.0F - openAmount) * (1.0F - openAmount) * (1.0F - openAmount))
                * Mth.HALF_PI * 0.9444444F;
    }
}
