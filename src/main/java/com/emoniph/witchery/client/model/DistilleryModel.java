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

public class DistilleryModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "distillery"), "main");

    private final ModelPart stillBase;
    private final ModelPart stillMiddle;
    private final ModelPart stillTop;
    private final ModelPart stillBend;
    private final ModelPart stillTube;
    private final ModelPart frameTop;
    private final ModelPart frameLeft;
    private final ModelPart frameRight;
    private final ModelPart frameBase;

    public DistilleryModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.stillBase = root.getChild("still_base");
        this.stillMiddle = root.getChild("still_middle");
        this.stillTop = root.getChild("still_top");
        this.stillBend = root.getChild("still_bend");
        this.stillTube = root.getChild("still_tube");
        this.frameTop = root.getChild("frame_top");
        this.frameLeft = root.getChild("frame_left");
        this.frameRight = root.getChild("frame_right");
        this.frameBase = root.getChild("frame_base");
    }

    // UV and geometry ported 1:1 from ModelDistillery (64x64 texture sheet)
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("still_base",
                CubeListBuilder.create().texOffs(0, 16).addBox(0f, 0f, 0f, 10, 6, 10),
                PartPose.offset(-5f, 18f, -2f));

        root.addOrReplaceChild("still_middle",
                CubeListBuilder.create().texOffs(0, 6).addBox(0f, 0f, 0f, 6, 4, 6),
                PartPose.offset(-3f, 14f, 0f));

        root.addOrReplaceChild("still_top",
                CubeListBuilder.create().texOffs(25, 9).addBox(0f, 0f, 0f, 4, 3, 4),
                PartPose.offset(-2f, 11f, 1f));

        root.addOrReplaceChild("still_bend",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 2, 2, 4),
                PartPose.offset(-1f, 9f, -1f));

        // stillTube has a rotation of -2.341978 rad around X
        root.addOrReplaceChild("still_tube",
                CubeListBuilder.create().texOffs(46, 10).addBox(-0.5f, -0.5f, 0f, 1, 1, 8),
                PartPose.offsetAndRotation(0f, 10f, 0f, -2.341978f, 0f, 0f));

        root.addOrReplaceChild("frame_top",
                CubeListBuilder.create().texOffs(30, 6).addBox(0f, 0f, 0f, 16, 1, 1),
                PartPose.offset(-8f, 15f, -6f));

        root.addOrReplaceChild("frame_left",
                CubeListBuilder.create().texOffs(47, 24).addBox(0f, 0f, 0f, 1, 7, 1),
                PartPose.offset(-8f, 16f, -6f));

        root.addOrReplaceChild("frame_right",
                CubeListBuilder.create().texOffs(47, 24).addBox(0f, 0f, 0f, 1, 7, 1),
                PartPose.offset(7f, 16f, -6f));

        root.addOrReplaceChild("frame_base",
                CubeListBuilder.create().texOffs(22, 0).addBox(0f, 0f, 0f, 16, 1, 5),
                PartPose.offset(-8f, 23f, -8f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    }
