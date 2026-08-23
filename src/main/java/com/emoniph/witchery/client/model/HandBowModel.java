package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class HandBowModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "hand_bow_held"), "main");

    public enum Bolt {
        NONE,
        STAKE,
        DRAINING,
        HOLY,
        SPLITTING,
        SILVER
    }

    private final ModelPart crossOuterRight;
    private final ModelPart crossInnerRight;
    private final ModelPart crossOuterLeft;
    private final ModelPart crossInnerLeft;
    private final ModelPart stringInnerRight;
    private final ModelPart stringMidRight;
    private final ModelPart stringOuterRight;
    private final ModelPart stringInnerLeft;
    private final ModelPart stringMidLeft;
    private final ModelPart stringOuterLeft;
    private final ModelPart stringCenter;
    private final ModelPart boltStake;
    private final ModelPart boltDraining;
    private final ModelPart boltHoly;
    private final ModelPart boltSplitting;
    private final ModelPart boltSplitting2;
    private final ModelPart boltSilver;

    public HandBowModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.crossOuterRight = root.getChild("cross_outer_right");
        this.crossInnerRight = root.getChild("cross_inner_right");
        this.crossOuterLeft = root.getChild("cross_outer_left");
        this.crossInnerLeft = root.getChild("cross_inner_left");
        this.stringInnerRight = root.getChild("string_inner_right");
        this.stringMidRight = root.getChild("string_mid_right");
        this.stringOuterRight = root.getChild("string_outer_right");
        this.stringInnerLeft = root.getChild("string_inner_left");
        this.stringMidLeft = root.getChild("string_mid_left");
        this.stringOuterLeft = root.getChild("string_outer_left");
        this.stringCenter = root.getChild("string_center");
        this.boltStake = root.getChild("bolt_stake");
        this.boltDraining = root.getChild("bolt_draining");
        this.boltHoly = root.getChild("bolt_holy");
        this.boltSplitting = root.getChild("bolt_splitting");
        this.boltSplitting2 = root.getChild("bolt_splitting_2");
        this.boltSilver = root.getChild("bolt_silver");
    }

    /** Stage 0 is source idle/early draw, 1 is ticks 6-10, and 2 is ticks 11+ or loaded. */
    public void setup(int stage, Bolt bolt) {
        if (stage >= 2) {
            setDrawPositions(-6.0F, -4.0F, -1.0F, 0.0F, -2.0F, 1.0F);
        } else if (stage == 1) {
            setDrawPositions(-6.0F, -5.0F, -2.0F, -2.0F, -3.0F, -1.0F);
        } else {
            setDrawPositions(-7.0F, -6.0F, -4.0F, -4.0F, -4.0F, -3.25F);
        }

        boltStake.visible = bolt == Bolt.STAKE;
        boltDraining.visible = bolt == Bolt.DRAINING;
        boltHoly.visible = bolt == Bolt.HOLY;
        boltSplitting.visible = bolt == Bolt.SPLITTING;
        boltSplitting2.visible = bolt == Bolt.SPLITTING;
        boltSilver.visible = bolt == Bolt.SILVER;
    }

    private void setDrawPositions(float innerCrossZ, float outerCrossZ, float innerStringZ,
                                  float midStringZ, float outerStringZ, float centerStringZ) {
        crossInnerLeft.z = crossInnerRight.z = innerCrossZ;
        crossOuterLeft.z = crossOuterRight.z = outerCrossZ;
        stringInnerLeft.z = stringInnerRight.z = innerStringZ;
        stringMidLeft.z = stringMidRight.z = midStringZ;
        stringOuterLeft.z = stringOuterRight.z = outerStringZ;
        stringCenter.z = centerStringZ;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        add(root, "stock_top", 2, 2, -1.0F, 0.0F, -5.0F, 2, 1, 7, 0.0F, 0.0F, 0.0F);
        add(root, "stock_bottom", 0, 10, -0.5F, 0.0F, -6.0F, 1, 1, 8, 0.0F, 1.0F, 0.0F);
        add(root, "stock_catch", 1, 11, -0.5F, 0.0F, 0.0F, 1, 1, 1, 0.0F, -1.0F, 1.0F);
        add(root, "grip", 0, 3, -0.5F, 0.0F, -1.0F, 1, 3, 2, 0.0F, 2.0F, 0.0F);
        add(root, "cross", 1, 19, -3.0F, 0.0F, 0.0F, 6, 1, 2, 0.0F, -0.3F, -7.0F);

        add(root, "cross_outer_right", 0, 14, -1.0F, 0.0F, 0.0F, 1, 1, 2, -4.0F, 0.0F, -4.0F);
        add(root, "cross_inner_right", 0, 14, -1.0F, 0.0F, 0.0F, 1, 1, 2, -3.0F, -0.15F, -6.0F);
        add(root, "cross_outer_left", 0, 14, 0.0F, 0.0F, 0.0F, 1, 1, 2, 4.0F, 0.0F, -4.0F);
        add(root, "cross_inner_left", 0, 14, 0.0F, 0.0F, 0.0F, 1, 1, 2, 3.0F, -0.15F, -6.0F);

        add(root, "string_inner_right", 0, 0, -1.0F, 0.0F, 0.0F, 1, 1, 1, -2.0F, -0.05F, -1.0F);
        add(root, "string_mid_right", 0, 0, -1.0F, 0.0F, 0.0F, 1, 1, 1, -1.0F, -0.1F, 0.0F);
        add(root, "string_outer_right", 0, 0, -1.0F, 0.0F, 0.0F, 1, 1, 1, -3.0F, 0.0F, -2.0F);
        add(root, "string_inner_left", 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 2.0F, -0.05F, -1.0F);
        add(root, "string_mid_left", 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 1.0F, -0.1F, 0.0F);
        add(root, "string_outer_left", 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 3.0F, 0.0F, -2.0F);
        root.addOrReplaceChild("string_center", mirrored(4, 0)
                        .addBox(-1.5F, 0.0F, -0.5F, 3, 1, 1),
                PartPose.offsetAndRotation(0.0F, -0.1F, 1.0F, 0.0F, 0.0174533F, 0.0F));

        add(root, "bolt_stake", 0, 22, -0.5F, 0.5F, -6.0F, 1, 1, 9, 0.0F, -1.0F, -2.0F);
        add(root, "bolt_draining", 20, 22, -0.5F, 0.5F, -6.0F, 1, 1, 9, 0.0F, -1.0F, -2.0F);
        add(root, "bolt_holy", 40, 22, -0.5F, 0.5F, -6.0F, 1, 1, 9, 0.0F, -1.0F, -2.0F);
        add(root, "bolt_splitting", 20, 12, -0.5F, 0.5F, -6.0F, 1, 1, 9, 0.0F, -1.0F, -2.0F);
        add(root, "bolt_splitting_2", 17, 11, -0.5F, 0.5F, -6.0F, 2, 1, 4, -0.5F, -1.5F, -1.0F);
        add(root, "bolt_silver", 40, 12, -0.5F, 0.5F, -6.0F, 1, 1, 9, 0.0F, -1.0F, -2.0F);

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void add(PartDefinition root, String name, int u, int v,
                            float boxX, float boxY, float boxZ, int width, int height, int depth,
                            float pivotX, float pivotY, float pivotZ) {
        root.addOrReplaceChild(name, mirrored(u, v).addBox(boxX, boxY, boxZ, width, height, depth),
                PartPose.offset(pivotX, pivotY, pivotZ));
    }

    private static CubeListBuilder mirrored(int u, int v) {
        return CubeListBuilder.create().mirror().texOffs(u, v);
    }
}
