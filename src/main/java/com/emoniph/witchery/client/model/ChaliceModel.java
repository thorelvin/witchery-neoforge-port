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

public class ChaliceModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "chalice"), "main");

    private final List<ModelPart> parts;

    public ChaliceModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        parts = List.of(
            root.getChild("side_right"), root.getChild("side_left"),
            root.getChild("side_back"), root.getChild("side_front"),
            root.getChild("side_bottom"), root.getChild("neck"), root.getChild("base")
        );
    }

    // Chalice cup sides — ported exactly from original including zero-dimension flat faces.
    // Negative UV offsets are intentional: UV(0,-5) causes clamping so the visible face
    // samples texture rows 0-4, exactly matching the original rendering.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cup walls — exact original UV and dimensions (including zero-size flat faces)
        root.addOrReplaceChild("side_right",  CubeListBuilder.create().texOffs(0,-5).addBox(0,0,0,0,4,5), PartPose.offset(3f,17f,-2f));
        root.addOrReplaceChild("side_left",   CubeListBuilder.create().texOffs(0,-5).addBox(0,0,0,0,4,5), PartPose.offset(-2f,17f,-2f));
        root.addOrReplaceChild("side_back",   CubeListBuilder.create().texOffs(0,0) .addBox(0,0,0,5,4,0), PartPose.offset(-2f,17f,3f));
        root.addOrReplaceChild("side_front",  CubeListBuilder.create().texOffs(0,0) .addBox(0,0,0,5,4,0), PartPose.offset(-2f,17f,-2f));
        root.addOrReplaceChild("side_bottom", CubeListBuilder.create().texOffs(-5,4).addBox(0,0,0,5,0,5), PartPose.offset(-2f,21f,-2f));
        root.addOrReplaceChild("neck",        CubeListBuilder.create().texOffs(4,10).addBox(0,0,0,1,2,1), PartPose.offset(0f,21f,0f));
        root.addOrReplaceChild("base",        CubeListBuilder.create().texOffs(0,13).addBox(0,0,0,3,1,3), PartPose.offset(-1f,23f,-1f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    }
