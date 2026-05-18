package com.emoniph.witchery.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class PoppetShelfModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "poppet_shelf"), "main");

    private final ModelPart table;

    public PoppetShelfModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entitySolid);
        this.table = root.getChild("table");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("table",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 16, 8, 16),
                PartPose.offset(0f, 0f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    }
