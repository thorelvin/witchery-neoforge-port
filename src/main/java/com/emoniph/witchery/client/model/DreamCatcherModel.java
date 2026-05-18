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

public class DreamCatcherModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "dream_catcher"), "main");

    private final List<ModelPart> parts;

    public DreamCatcherModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        parts = List.of(
            root.getChild("frame_left"), root.getChild("frame_right"),
            root.getChild("frame_top"), root.getChild("frame_bottom"),
            root.getChild("net"), root.getChild("decoration")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("frame_left",   CubeListBuilder.create().texOffs(0,2).addBox(0,0,0,1,8,1), PartPose.offset(-4f,10f,7f));
        root.addOrReplaceChild("frame_right",  CubeListBuilder.create().texOffs(0,2).addBox(0,0,0,1,8,1), PartPose.offset(3f,10f,7f));
        root.addOrReplaceChild("frame_top",    CubeListBuilder.create().texOffs(0,0).addBox(0,0,0,6,1,1), PartPose.offset(-3f,10f,7f));
        root.addOrReplaceChild("frame_bottom", CubeListBuilder.create().texOffs(0,0).addBox(0,0,0,6,1,1), PartPose.offset(-3f,17f,7f));
        // Net and decoration are D=0 flat faces — use original zero dimension for correct UV
        root.addOrReplaceChild("net",          CubeListBuilder.create().texOffs(0,12).addBox(0,0,0,6,6,0), PartPose.offset(-3f,11f,7f));
        root.addOrReplaceChild("decoration",   CubeListBuilder.create().texOffs(0,12).addBox(0,0,0,8,6,0), PartPose.offset(-4f,18f,7f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    }
