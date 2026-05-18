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

public class SpinningWheelModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "spinning_wheel"), "main");

    private final List<ModelPart> parts;

    public SpinningWheelModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        parts = List.of(
            root.getChild("seat"),
            root.getChild("leg_back_right"), root.getChild("leg_back_left"),
            root.getChild("leg_front_right"), root.getChild("leg_front_left"),
            root.getChild("thread"), root.getChild("thread_pole"),
            root.getChild("arm_right"), root.getChild("arm_left"),
            root.getChild("wheel_group")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("seat",            CubeListBuilder.create().texOffs(0,0) .addBox(-2f,-1f,-7f,4,1,14), PartPose.offsetAndRotation(0f,18f,0f, 0.2602503f,0,0));
        root.addOrReplaceChild("leg_back_right",  CubeListBuilder.create().texOffs(32,0).addBox(-1f,0,0,1,9,1),      PartPose.offsetAndRotation(-1f,16f,5f, 0.1745329f,0,0.1745329f));
        root.addOrReplaceChild("leg_back_left",   CubeListBuilder.create().texOffs(32,0).addBox(0,0,0,1,9,1),        PartPose.offsetAndRotation(1f,16f,5f, 0.1745329f,0,-0.1745329f));
        root.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(0,6) .addBox(-1f,0,0,1,6,1),      PartPose.offsetAndRotation(-1f,19f,-6f, -0.1745329f,0,0.1745329f));
        root.addOrReplaceChild("leg_front_left",  CubeListBuilder.create().texOffs(0,6) .addBox(0,0,0,1,6,1),        PartPose.offsetAndRotation(1f,19f,-6f, -0.1745329f,0,-0.1745329f));
        root.addOrReplaceChild("thread",          CubeListBuilder.create().texOffs(23,0).addBox(-1f,-3f,-1f,2,3,2),  PartPose.offset(0f,12f,5f));
        root.addOrReplaceChild("thread_pole",     CubeListBuilder.create().texOffs(9,7) .addBox(-0.5f,0,-0.5f,1,4,1),PartPose.offset(0f,12f,5f));
        root.addOrReplaceChild("arm_right",       CubeListBuilder.create().texOffs(28,6).addBox(-0.5f,-7f,-0.5f,1,7,1),PartPose.offsetAndRotation(-1f,18f,-2f, 0.2268928f,0,0));
        root.addOrReplaceChild("arm_left",        CubeListBuilder.create().texOffs(28,6).addBox(-0.5f,-7f,-0.5f,1,7,1),PartPose.offsetAndRotation(1f,18f,-2f, 0.2268928f,0,0));

        // Wheel sub-group at offset(0,12,-3.5) with children ported from original
        PartDefinition wheelGroup = root.addOrReplaceChild("wheel_group",
                CubeListBuilder.create(), PartPose.offset(0f, 12f, -3.5f));

        // spokes: W=0 flat face, UV(0,-6) → face visible region rows 0-6
        wheelGroup.addOrReplaceChild("wheel_spokes",
                CubeListBuilder.create().texOffs(0,-6).addBox(0f,-3f,-3f, 0,6,6), PartPose.ZERO);
        // top/bottom rim bars
        wheelGroup.addOrReplaceChild("wheel_top",
                CubeListBuilder.create().texOffs(0,7).addBox(-0.5f,-4f,-3f, 1,1,6), PartPose.ZERO);
        wheelGroup.addOrReplaceChild("wheel_bottom",
                CubeListBuilder.create().texOffs(0,7).addBox(-0.5f,3f,-3f, 1,1,6), PartPose.ZERO);
        // front/back vertical posts of the wheel rim
        wheelGroup.addOrReplaceChild("wheel_back",
                CubeListBuilder.create().texOffs(23,5).addBox(-0.5f,-4f,3f, 1,8,1), PartPose.ZERO);
        wheelGroup.addOrReplaceChild("wheel_front",
                CubeListBuilder.create().texOffs(23,5).addBox(-0.5f,-4f,-4f, 1,8,1), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    }
