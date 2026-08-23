package com.emoniph.witchery.client.model;

import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Exact 29-part graph from Witchery 0.24.1 {@code ModelGarlicGarland}. */
public final class GarlicGarlandModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "garlic_garland"), "main");
    public static final float GARLIC_SCALE = 0.21F;
    public static final float STRING_ANGLE = 0.5462881F;

    private final List<ModelPart> garlicRoots;

    public GarlicGarlandModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        garlicRoots = List.of(
                root.getChild("garlic_c"),
                root.getChild("garlic_d"),
                root.getChild("garlic_e"),
                root.getChild("garlic_a"),
                root.getChild("garlic_b"));
        for (ModelPart garlic : garlicRoots) {
            garlic.xScale = GARLIC_SCALE;
            garlic.yScale = GARLIC_SCALE;
            garlic.zScale = GARLIC_SCALE;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        addGarlic(root, "garlic_c", 0.0F, 0.0F, 7.0F, 4, 3, 1, 2);
        addGarlic(root, "garlic_d", 2.5F, 1.5F, 7.0F, 1, 3, 4, 2);
        addString(root, "string_2", -3.0F, 1.8F, 7.0F, -STRING_ANGLE);
        addGarlic(root, "garlic_e", 5.0F, 0.0F, 7.0F, 1, 3, 4, 2);
        addString(root, "string_3", -0.4F, -0.3F, 7.0F, STRING_ANGLE);
        addString(root, "string_1", -5.4F, -0.3F, 7.0F, STRING_ANGLE);
        addString(root, "string_4", 2.0F, 1.8F, 7.0F, -STRING_ANGLE);
        addGarlic(root, "garlic_a", -5.0F, 0.0F, 7.0F, 4, 1, 2, 3);
        addGarlic(root, "garlic_b", -2.5F, 1.5F, 7.0F, 3, 1, 2, 4);

        return LayerDefinition.create(mesh, 32, 32);
    }

    public List<ModelPart> garlicRoots() {
        return garlicRoots;
    }

    private static void addGarlic(PartDefinition root, String name, float x, float y, float z,
                                  int... childOrder) {
        PartDefinition garlic = root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1),
                PartPose.offset(x, y, z));
        for (int child : childOrder) {
            garlic.addOrReplaceChild("garlic_" + child, bulb(child), PartPose.ZERO);
        }
    }

    private static CubeListBuilder bulb(int layer) {
        return switch (layer) {
            case 1 -> CubeListBuilder.create().texOffs(0, 3)
                    .addBox(-1.5F, 2.0F, -1.5F, 3, 1, 3);
            case 2 -> CubeListBuilder.create().texOffs(0, 7)
                    .addBox(-2.5F, 3.0F, -2.5F, 5, 1, 5);
            case 3 -> CubeListBuilder.create().texOffs(0, 13)
                    .addBox(-3.5F, 4.0F, -3.5F, 7, 3, 7);
            case 4 -> CubeListBuilder.create().texOffs(0, 23)
                    .addBox(-2.0F, 7.0F, -2.0F, 4, 1, 4);
            default -> throw new IllegalArgumentException("Unknown garlic bulb layer " + layer);
        };
    }

    private static void addString(PartDefinition root, String name,
                                  float x, float y, float z, float zRot) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(6, 0)
                        .addBox(0.0F, -0.5F, -0.5F, 4, 1, 1,
                                new CubeDeformation(-0.4F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }
}
