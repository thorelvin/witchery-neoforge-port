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
import net.minecraft.resources.Identifier;

/** Exact 19-part graph from Witchery 0.24.1 {@code ModelMirror}. */
public final class MirrorModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "mirror"), "main");

    private final List<ModelPart> sourceParts;
    private final ModelPart backMiddle;

    public MirrorModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        sourceParts = List.of(
                root.getChild("frame_inner_right"),
                root.getChild("frame_outer_curve_lower_left"),
                root.getChild("frame_outer_left"),
                root.getChild("back_right"),
                root.getChild("back_left"),
                root.getChild("frame_outer_top"),
                root.getChild("frame_inner_left"),
                root.getChild("frame_inner_curve_lower_left"),
                root.getChild("frame_inner_curve_lower_right"),
                root.getChild("frame_outer_right"),
                root.getChild("frame_outer_curve_upper_left"),
                root.getChild("frame_inner_curve_upper_left"),
                root.getChild("frame_outer_curve_lower_right"),
                root.getChild("frame_inner_top"),
                root.getChild("frame_inner_curve_upper_right"),
                root.getChild("frame_outer_curve_mid_right"),
                root.getChild("frame_outer_curve_upper_right"),
                root.getChild("frame_outer_curve_mid_left"),
                root.getChild("back_middle"));
        backMiddle = root.getChild("back_middle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        add(root, "frame_inner_right", 5, 5, -6.0F, -2.0F, -1.0F, 1, 10, 1, false, 0.0F);
        add(root, "frame_outer_curve_lower_left", 5, 0, 7.0F, -5.0F, -1.0F, 1, 2, 1, true, 0.0F);
        add(root, "frame_outer_left", 0, 0, 6.5F, -3.0F, -1.0F, 1, 11, 1, true, 0.0F);
        add(root, "back_right", 0, 17, -7.0F, -6.0F, -1.0F, 2, 14, 1, false, 1.0F);
        add(root, "back_left", 0, 17, 5.0F, -6.0F, -1.0F, 2, 14, 1, true, 1.0F);
        add(root, "frame_outer_top", 4, 3, -5.0F, -7.5F, -1.0F, 10, 1, 1, false, 0.0F);
        add(root, "frame_inner_left", 5, 5, 5.0F, -2.0F, -1.0F, 1, 10, 1, true, 0.0F);
        add(root, "frame_inner_curve_lower_left", 10, 6, 4.0F, -3.0F, -1.0F, 1, 2, 1, true, 0.0F);
        add(root, "frame_inner_curve_lower_right", 10, 6, -5.0F, -3.0F, -1.0F, 1, 2, 1, false, 0.0F);
        add(root, "frame_outer_right", 0, 0, -7.5F, -3.0F, -1.0F, 1, 11, 1, false, 0.0F);
        add(root, "frame_outer_curve_upper_left", 17, 0, 5.0F, -7.0F, -1.0F, 1, 1, 1, true, 0.0F);
        add(root, "frame_inner_curve_upper_left", 15, 6, 3.0F, -4.0F, -1.0F, 1, 2, 1, true, 0.0F);
        add(root, "frame_outer_curve_lower_right", 5, 0, -8.0F, -5.0F, -1.0F, 1, 2, 1, false, 0.0F);
        add(root, "frame_inner_top", 10, 10, -3.0F, -4.0F, -1.0F, 6, 1, 1, false, 0.0F);
        add(root, "frame_inner_curve_upper_right", 15, 6, -4.0F, -4.0F, -1.0F, 1, 2, 1, false, 0.0F);
        add(root, "frame_outer_curve_mid_right", 10, 0, -7.5F, -6.0F, -1.0F, 2, 1, 1, false, 0.0F);
        add(root, "frame_outer_curve_upper_right", 17, 0, -6.0F, -7.0F, -1.0F, 1, 1, 1, false, 0.0F);
        add(root, "frame_outer_curve_mid_left", 10, 0, 5.5F, -6.0F, -1.0F, 2, 1, 1, true, 0.0F);
        add(root, "back_middle", 7, 16, -5.0F, -7.0F, -1.0F, 10, 15, 1, false, 1.0F);

        return LayerDefinition.create(mesh, 32, 32);
    }

    public List<ModelPart> sourceParts() {
        return sourceParts;
    }

    public ModelPart backMiddle() {
        return backMiddle;
    }

    private static void add(PartDefinition root, String name, int u, int v,
                            float x, float y, float z, int width, int height, int depth,
                            boolean mirrored, float pivotZ) {
        CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
        if (mirrored) {
            cube.mirror();
        }
        root.addOrReplaceChild(name, cube.addBox(x, y, z, width, height, depth),
                PartPose.offset(0.0F, 0.0F, pivotZ));
    }
}
