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

public class FumeFunnelModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "fume_funnel"), "main");

    private final List<ModelPart> parts;

    public FumeFunnelModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        parts = List.of(
            root.getChild("base"), root.getChild("body"),
            root.getChild("tube_left"), root.getChild("tube_right"),
            root.getChild("pipe_top2"), root.getChild("pipe_top3"),
            root.getChild("pipe_top4"), root.getChild("pipe_top5"),
            root.getChild("pipe_top1"), root.getChild("top1"), root.getChild("top2"),
            root.getChild("pipe_bottom5"),
            root.getChild("filter_left"), root.getChild("filter_right"),
            root.getChild("filter_mid"), root.getChild("filter_case"),
            root.getChild("chimney"), root.getChild("chimney_top")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("base",         CubeListBuilder.create().texOffs(0,51) .addBox(0,0,0,12,1,12),  PartPose.offset(-6f,23f,-6f));
        root.addOrReplaceChild("body",         CubeListBuilder.create().texOffs(4,27) .addBox(0,0,0,10,11,10), PartPose.offset(-5f,12f,-5f));
        root.addOrReplaceChild("tube_left",    CubeListBuilder.create().texOffs(1,18) .addBox(0,0,0,5,2,2),    PartPose.offset(-10f,17f,-1f));
        root.addOrReplaceChild("tube_right",   CubeListBuilder.create().texOffs(1,18) .addBox(0,1,0,5,2,2),    PartPose.offset(5f,18f,1f));
        root.addOrReplaceChild("pipe_top2",    CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,1,1,6),    PartPose.offset(-4f,8f,-3f));
        root.addOrReplaceChild("pipe_top3",    CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,11,1,1),   PartPose.offset(-3f,8f,-3f));
        root.addOrReplaceChild("pipe_top4",    CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,1,11,1),   PartPose.offset(7f,9f,-3f));
        root.addOrReplaceChild("pipe_top5",    CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,2,3,3),    PartPose.offset(5f,18f,-4f));
        root.addOrReplaceChild("pipe_top1",    CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,1,3,1),    PartPose.offset(-4f,8f,3f));
        root.addOrReplaceChild("top1",         CubeListBuilder.create().texOffs(0,51) .addBox(0,0,0,12,1,12),  PartPose.offset(-6f,11f,-6f));
        root.addOrReplaceChild("top2",         CubeListBuilder.create().texOffs(37,55).addBox(0,0,0,6,1,6),    PartPose.offset(-3f,10f,-3f));
        root.addOrReplaceChild("pipe_bottom5", CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,1,7,1),    PartPose.offset(-7f,14f,-3f));
        root.addOrReplaceChild("filter_left",  CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,1,1,2),    PartPose.offset(-4f,14f,-7f));
        root.addOrReplaceChild("filter_right", CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,1,1,2),    PartPose.offset(3f,14f,-7f));
        root.addOrReplaceChild("filter_mid",   CubeListBuilder.create().texOffs(24,0) .addBox(0,0,0,6,1,1),    PartPose.offset(-3f,14f,-7f));
        root.addOrReplaceChild("filter_case",  CubeListBuilder.create().texOffs(25,3) .addBox(0,0,0,4,3,2),    PartPose.offset(-2f,13f,-8f));
        root.addOrReplaceChild("chimney",      CubeListBuilder.create().texOffs(27,13).addBox(0,0,0,4,10,4),   PartPose.offset(-2f,14f,3f));
        root.addOrReplaceChild("chimney_top",  CubeListBuilder.create().texOffs(40,7) .addBox(0,0,0,6,3,6),    PartPose.offset(-3f,11f,2f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    }
