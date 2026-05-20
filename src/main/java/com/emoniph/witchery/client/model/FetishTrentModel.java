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

public class FetishTrentModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "fetish_trent"), "main");

    public FetishTrentModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(18, 1).addBox(-3.0F, 1.0F, -2.9F, 6.0F, 7.0F, 0.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(2.0F, 13.0F, 0.0F, -0.1858931F, 0.0F, -0.7435722F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, 13.0F, 0.0F, -0.1858931F, 0.0F, 0.8551081F));
        root.addOrReplaceChild("leg_left_upper",
                CubeListBuilder.create().texOffs(9, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(2.0F, 18.0F, 0.0F, -0.1487144F, 0.0F, -0.2602503F));
        root.addOrReplaceChild("leg_left_lower",
                CubeListBuilder.create().texOffs(11, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, 21.0F, -0.5F, 0.0743572F, 0.0F, -0.1115358F));
        root.addOrReplaceChild("leg_right_upper",
                CubeListBuilder.create().texOffs(9, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, 18.0F, 0.0F, 0.1858931F, 0.0F, 0.3346075F));
        root.addOrReplaceChild("leg_right_lower",
                CubeListBuilder.create().texOffs(11, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 21.0F, 0.5F, 0.1858931F, 0.0F, 0.2230717F));
        root.addOrReplaceChild("headdress_1",
                CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, 1.0F, 0.1115358F, 0.0F, -2.862753F));
        root.addOrReplaceChild("headdress_2",
                CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-1.0F, 13.0F, 0.0F, 0.3717861F, 0.0F, 2.639681F));
        root.addOrReplaceChild("headdress_3",
                CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-1.0F, 13.0F, 0.0F, -0.4461433F, 0.0F, 2.862753F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
