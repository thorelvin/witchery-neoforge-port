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

public class CandelabraModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "candelabra"), "main");

    private final ModelPart candleLeft, candleRight, candleFront, candleBack, candleMiddle;
    private final ModelPart supportLR, supportFB;
    private final ModelPart sconceLeft, sconceRight, sconceFront, sconceBack, sconceMiddle;
    private final ModelPart baseTop, baseBottom;

    public CandelabraModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        candleLeft   = root.getChild("candle_left");
        candleRight  = root.getChild("candle_right");
        candleFront  = root.getChild("candle_front");
        candleBack   = root.getChild("candle_back");
        candleMiddle = root.getChild("candle_middle");
        supportLR    = root.getChild("support_lr");
        supportFB    = root.getChild("support_fb");
        sconceLeft   = root.getChild("sconce_left");
        sconceRight  = root.getChild("sconce_right");
        sconceFront  = root.getChild("sconce_front");
        sconceBack   = root.getChild("sconce_back");
        sconceMiddle = root.getChild("sconce_middle");
        baseTop      = root.getChild("base_top");
        baseBottom   = root.getChild("base_bottom");
    }

    // UV and geometry ported 1:1 from ModelCandelabra (32x32 texture sheet)
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("candle_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 2, 8, 2),
                PartPose.offset(-6f, 11f, -1f));
        root.addOrReplaceChild("candle_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 2, 8, 2),
                PartPose.offset(4f, 11f, -1f));
        root.addOrReplaceChild("candle_front",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 2, 8, 2),
                PartPose.offset(-1f, 11f, -6f));
        root.addOrReplaceChild("candle_back",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 2, 8, 2),
                PartPose.offset(-1f, 11f, 4f));
        root.addOrReplaceChild("candle_middle",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 2, 13, 2),
                PartPose.offset(-1f, 9f, -1f));

        root.addOrReplaceChild("support_lr",
                CubeListBuilder.create().texOffs(0, 17).addBox(0f, 0f, 0f, 12, 1, 2),
                PartPose.offset(-6f, 19f, -1f));
        root.addOrReplaceChild("support_fb",
                CubeListBuilder.create().texOffs(0, 4).addBox(0f, 0f, 0f, 2, 1, 12),
                PartPose.offset(-1f, 19f, -6f));

        root.addOrReplaceChild("sconce_left",
                CubeListBuilder.create().texOffs(0, 20).addBox(0f, 0f, 0f, 3, 1, 3),
                PartPose.offset(-6.5f, 17f, -1.5f));
        root.addOrReplaceChild("sconce_right",
                CubeListBuilder.create().texOffs(0, 20).addBox(0f, 0f, 0f, 3, 1, 3),
                PartPose.offset(3.5f, 17f, -1.5f));
        root.addOrReplaceChild("sconce_front",
                CubeListBuilder.create().texOffs(0, 20).addBox(0f, 0f, 0f, 3, 1, 3),
                PartPose.offset(-1.5f, 17f, -6.5f));
        root.addOrReplaceChild("sconce_back",
                CubeListBuilder.create().texOffs(0, 20).addBox(0f, 0f, 0f, 3, 1, 3),
                PartPose.offset(-1.5f, 17f, 3.5f));
        root.addOrReplaceChild("sconce_middle",
                CubeListBuilder.create().texOffs(0, 20).addBox(0f, 0f, 0f, 3, 1, 3),
                PartPose.offset(-1.5f, 15f, -1.5f));

        root.addOrReplaceChild("base_top",
                CubeListBuilder.create().texOffs(12, 20).addBox(0f, 0f, 0f, 3, 1, 3),
                PartPose.offset(-1.5f, 22f, -1.5f));
        root.addOrReplaceChild("base_bottom",
                CubeListBuilder.create().texOffs(8, 24).addBox(-2.5f, 0f, -2.5f, 5, 1, 5),
                PartPose.offset(0f, 23f, 0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    }
