package com.emoniph.witchery.client.model;

import java.util.List;
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

/** Exact 1.7.10 Blood Crucible body plus an equivalent full-sprite liquid plane. */
public class BloodCrucibleModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "blood_crucible"), "main");
    public static final ModelLayerLocation LIQUID_LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "blood_crucible"), "liquid");

    private final List<ModelPart> bodyParts;
    private final ModelPart blood;

    public BloodCrucibleModel(ModelPart root, ModelPart liquidRoot) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.bodyParts = List.of(
                root.getChild("left_lower"),
                root.getChild("right_lower"),
                root.getChild("front_lower"),
                root.getChild("front_upper"),
                root.getChild("right_upper"),
                root.getChild("bottom"),
                root.getChild("back_lower"),
                root.getChild("back_upper"),
                root.getChild("left_upper"));
        this.blood = liquidRoot.getChild("blood");
        this.blood.xScale = 0.375F;
        this.blood.zScale = 0.375F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        add(root, "left_lower", 17, 11, 2.0F, -2.0F, -2.0F, 1, 1, 4);
        add(root, "right_lower", 17, 11, -3.0F, -2.0F, -2.0F, 1, 1, 4);
        add(root, "front_lower", 17, 19, -3.0F, -2.0F, -3.0F, 6, 1, 1);
        add(root, "front_upper", 0, 17, -3.5F, -5.0F, -4.0F, 7, 3, 1);
        add(root, "right_upper", 0, 6, -4.0F, -5.0F, -3.7F, 1, 3, 7);
        add(root, "bottom", 0, 0, -2.0F, -1.0F, -2.0F, 4, 1, 4);
        add(root, "back_lower", 17, 19, -3.0F, -2.0F, 2.0F, 6, 1, 1);
        add(root, "back_upper", 0, 17, -3.5F, -5.0F, 3.0F, 7, 3, 1);
        add(root, "left_upper", 0, 6, 3.0F, -5.0F, -3.5F, 1, 3, 7);

        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createLiquidLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("blood", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 16, 0, 16),
                PartPose.offset(-3.0F, 0.0F, -3.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static void add(PartDefinition root, String name, int u, int v,
                            float x, float y, float z, int width, int height, int depth) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                        .addBox(x, y, z, width, height, depth),
                LegacyModelTransforms.render(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
    }

    public List<ModelPart> bodyParts() {
        return bodyParts;
    }

    public ModelPart blood() {
        return blood;
    }

    public void setBloodPercent(float percentFilled) {
        blood.y = BloodCrucibleModel.bloodModelY(percentFilled);
    }

    static float bloodModelY(float percentFilled) {
        return -1.6F - 3.2F * percentFilled;
    }
}
