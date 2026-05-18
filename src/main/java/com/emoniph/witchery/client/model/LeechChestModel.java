package com.emoniph.witchery.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import java.util.List;

public class LeechChestModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "leech_chest"), "main");

    private final List<ModelPart> parts;

    public LeechChestModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        parts = List.of(
            root.getChild("chest_below"),
            root.getChild("chest_lid_bl"), root.getChild("chest_lid_fr"),
            root.getChild("chest_lid_br"), root.getChild("chest_lid_fl"),
            root.getChild("sac1"), root.getChild("sac2"), root.getChild("sac3")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("chest_below",  CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,14,9,14),    PartPose.offset(1f,7f,1f));
        root.addOrReplaceChild("chest_lid_bl", CubeListBuilder.create().texOffs(28,24).addBox(-6f,-5f,-6f,6,5,6), PartPose.offset(14f,7f,14f));
        root.addOrReplaceChild("chest_lid_fr", CubeListBuilder.create().texOffs(0,36) .addBox(0,-5f,0,6,5,6),     PartPose.offset(2f,7f,2f));
        root.addOrReplaceChild("chest_lid_br", CubeListBuilder.create().texOffs(0,24) .addBox(0,-5f,-6f,6,5,6),   PartPose.offset(2f,7f,14f));
        root.addOrReplaceChild("chest_lid_fl", CubeListBuilder.create().texOffs(28,36).addBox(-6f,-5f,0,6,5,6),   PartPose.offset(14f,7f,2f));
        root.addOrReplaceChild("sac1",         CubeListBuilder.create().texOffs(0,8)  .addBox(0,0,0,2,3,1),       PartPose.offset(3f,8f,0f));
        root.addOrReplaceChild("sac2",         CubeListBuilder.create().texOffs(0,3)  .addBox(0,0,0,3,2,1),       PartPose.offset(9f,13f,0f));
        root.addOrReplaceChild("sac3",         CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,2,1,1),       PartPose.offset(9f,9f,0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    }
