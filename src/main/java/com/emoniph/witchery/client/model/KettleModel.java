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

public class KettleModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "kettle"), "main");

    private final ModelPart sideFront;
    private final ModelPart sideBack;
    private final ModelPart sideLeft;
    private final ModelPart sideRight;
    private final ModelPart sideBottom;
    private final ModelPart crossbar;

    public KettleModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.sideFront = root.getChild("side_front");
        this.sideBack = root.getChild("side_back");
        this.sideLeft = root.getChild("side_left");
        this.sideRight = root.getChild("side_right");
        this.sideBottom = root.getChild("side_bottom");
        this.crossbar = root.getChild("crossbar");
    }

    // UV and geometry ported 1:1 from ModelKettle (64x64 texture sheet)
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("side_front",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offset(-5f, 18f, -5f));

        root.addOrReplaceChild("side_back",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offset(-4f, 18f, 4f));

        // sideLeft/Right are rotated 90° around Y (1.570796 rad)
        root.addOrReplaceChild("side_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offsetAndRotation(-5f, 18f, 5f, 0f, 1.570796f, 0f));

        root.addOrReplaceChild("side_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offsetAndRotation(4f, 18f, 4f, 0f, 1.570796f, 0f));

        root.addOrReplaceChild("side_bottom",
                CubeListBuilder.create().texOffs(13, 0).addBox(0f, 0f, 0f, 8, 1, 8),
                PartPose.offset(-4f, 23f, -4f));

        root.addOrReplaceChild("crossbar",
                CubeListBuilder.create().texOffs(0, 10).addBox(-4f, 0f, 0f, 24, 2, 2),
                PartPose.offset(-8f, 8.05f, -1f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    }
