package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;

public class KettleModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "kettle"), "main");

    private final List<ModelPart> bodyParts;
    private final List<ModelPart> chainParts;
    private final ModelPart crossbar;
    private final ModelPart bottle1;
    private final ModelPart bottle2;
    private final ModelPart[] liquid;

    public KettleModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        this.bodyParts = List.of(
                root.getChild("side_front"),
                root.getChild("side_back"),
                root.getChild("side_left"),
                root.getChild("side_right"),
                root.getChild("side_bottom"));
        this.chainParts = List.of(
                root.getChild("chain_lf"),
                root.getChild("chain_lb"),
                root.getChild("chain_rf"),
                root.getChild("chain_rb"));
        this.crossbar = root.getChild("crossbar");
        this.bottle1 = root.getChild("bottle1");
        this.bottle2 = root.getChild("bottle2");
        this.liquid = new ModelPart[] {
                root.getChild("liquid_0"),
                root.getChild("liquid_1"),
                root.getChild("liquid_2"),
                root.getChild("liquid_3"),
                root.getChild("liquid_4"),
                root.getChild("liquid_5"),
                root.getChild("liquid_6"),
                root.getChild("liquid_7")
        };
    }

    public Iterable<ModelPart> bodyParts() {
        return bodyParts;
    }

    public Iterable<ModelPart> chainParts() {
        return chainParts;
    }

    public ModelPart crossbar() {
        return crossbar;
    }

    public ModelPart bottle1() {
        return bottle1;
    }

    public ModelPart bottle2() {
        return bottle2;
    }

    public ModelPart liquid(int frame) {
        return liquid[Math.floorMod(frame, liquid.length)];
    }

    // UV and geometry ported from original Witchery 1.7.10 ModelKettle.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("side_front",
                mirrored(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offset(-5f, 18f, -5f));

        root.addOrReplaceChild("side_back",
                mirrored(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offset(-4f, 18f, 4f));

        root.addOrReplaceChild("side_left",
                mirrored(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offsetAndRotation(-5f, 18f, 5f, 0f, 1.570796f, 0f));

        root.addOrReplaceChild("side_right",
                mirrored(0, 0).addBox(0f, 0f, 0f, 9, 6, 1),
                PartPose.offsetAndRotation(4f, 18f, 4f, 0f, 1.570796f, 0f));

        root.addOrReplaceChild("side_bottom",
                mirrored(13, 0).addBox(0f, 0f, 0f, 8, 1, 8),
                PartPose.offset(-4f, 23f, -4f));

        root.addOrReplaceChild("crossbar",
                mirrored(0, 10).addBox(-4f, 0f, 0f, 24, 2, 2),
                PartPose.offset(-8f, 8.05f, -1f));

        for (int i = 0; i < 8; i++) {
            int u = i < 4 ? i * 16 - 8 : (i - 4) * 16 - 8;
            int v = i < 4 ? 16 : 32;
            root.addOrReplaceChild("liquid_" + i,
                    mirrored(u, v).addBox(0f, 0f, 0f, 8, 0, 8),
                    PartPose.offset(-4f, 20f, -4f));
        }

        root.addOrReplaceChild("chain_lf",
                mirrored(0, 15).addBox(0f, -0.5f, 0f, 11, 1, 0),
                PartPose.offsetAndRotation(0f, 9f, 0f, 0f, -2.75f, -1.1f));
        root.addOrReplaceChild("chain_lb",
                mirrored(0, 15).addBox(0f, -0.5f, 0f, 11, 1, 0),
                PartPose.offsetAndRotation(0f, 9f, 0f, 0f, 0.4f, 1.1f));
        root.addOrReplaceChild("chain_rf",
                mirrored(0, 15).addBox(0f, -0.5f, 0f, 11, 1, 0),
                PartPose.offsetAndRotation(0f, 9f, 0f, 0f, 0.4f, 2.05f));
        root.addOrReplaceChild("chain_rb",
                CubeListBuilder.create().texOffs(0, 15).addBox(0f, -0.5f, 0f, 11, 1, 0),
                PartPose.offsetAndRotation(0f, 9f, 0f, 0f, -0.4f, 1.1f));

        root.addOrReplaceChild("bottle1", bottleBuilder(),
                PartPose.offset(-4f, 13f, -6f));
        root.addOrReplaceChild("bottle2", bottleBuilder(),
                PartPose.offset(0f, 13f, -6f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder mirrored(int u, int v) {
        return CubeListBuilder.create().mirror().texOffs(u, v);
    }

    private static CubeListBuilder bottleBuilder() {
        return CubeListBuilder.create().mirror()
                .texOffs(52, 5).addBox(0f, 2f, 0f, 3, 3, 3)
                .texOffs(60, 3).addBox(1f, 1f, 1f, 1, 1, 1)
                .texOffs(56, 0).addBox(0.5f, 0f, 0.5f, 2, 1, 2);
    }
}
