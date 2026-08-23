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

public class WitchesOvenModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "witches_oven"), "main");

    private final ModelPart body;
    private final ModelPart lidBottom;
    private final ModelPart lidTop;
    private final ModelPart chimney;
    private final ModelPart chimneyTop;
    private final ModelPart legBackRight;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legFrontLeft;

    public WitchesOvenModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.lidBottom = root.getChild("lid_bottom");
        this.lidTop = root.getChild("lid_top");
        this.chimney = root.getChild("chimney");
        this.chimneyTop = root.getChild("chimney_top");
        this.legBackRight = root.getChild("leg_back_right");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legFrontLeft = root.getChild("leg_front_left");
    }

    // UV and geometry ported 1:1 from ModelWitchesOven (64x64 texture sheet)
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 1f, 0f, 12, 8, 12),
                PartPose.offset(-6f, 14f, -6f));

        root.addOrReplaceChild("lid_bottom",
                CubeListBuilder.create().texOffs(0, 20).addBox(0f, 0f, 0f, 14, 1, 14),
                PartPose.offset(-7f, 14f, -7f));

        root.addOrReplaceChild("lid_top",
                CubeListBuilder.create().texOffs(8, 35).addBox(0f, 0f, 0f, 10, 1, 10),
                PartPose.offset(-5f, 13f, -5f));

        root.addOrReplaceChild("chimney",
                CubeListBuilder.create().texOffs(48, 0).addBox(0f, 0f, 0f, 4, 13, 4),
                PartPose.offset(-2f, 8f, 3f));

        root.addOrReplaceChild("chimney_top",
                CubeListBuilder.create().texOffs(38, 0).addBox(0f, 0f, 0f, 4, 4, 1),
                PartPose.offset(-2f, 8f, 7f));

        // Each leg has a horizontal foot and a vertical post, matching original UV (0,0) and (0,2)
        root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-2f, 0f, 0f, 2, 1, 1)
                        .texOffs(0, 2).addBox(-3f, 0f, 0f, 1, 3, 1),
                PartPose.offset(-5f, 21f, -7f));

        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-2f, 0f, 0f, 2, 1, 1)
                        .texOffs(0, 2).addBox(-3f, 0f, 0f, 1, 3, 1),
                PartPose.offset(-5f, 21f, 6f));

        root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(0f, 0f, 0f, 2, 1, 1)
                        .texOffs(0, 2).addBox(2f, 0f, 0f, 1, 3, 1),
                PartPose.offset(5f, 21f, -7f));

        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(0f, 0f, 0f, 2, 1, 1)
                        .texOffs(0, 2).addBox(2f, 0f, 0f, 1, 3, 1),
                PartPose.offset(5f, 21f, 6f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    }
