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

/**
 * Bowl shape matching BlockBloodCrucible AABB: x=4..12, y=0..5, z=4..12 (in MC pixel coords).
 * Texture: 32×64.
 * Rendered with the standard 180° Z-flip pose (same as Kettle/Chalice).
 */
public class BloodCrucibleModel extends Model.Simple {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "blood_crucible"), "main");

    private final List<ModelPart> bodyParts;
    private final ModelPart blood;

    public BloodCrucibleModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        bodyParts = List.of(
                root.getChild("front_wall"),
                root.getChild("back_wall"),
                root.getChild("left_wall"),
                root.getChild("right_wall"),
                root.getChild("bottom")
        );
        blood = root.getChild("blood");
    }

    public Iterable<ModelPart> bodyParts() { return bodyParts; }
    public ModelPart blood() { return blood; }

    /**
     * UV layout (texture 32×64):
     *   front/back walls (8,5,1): 18w × 7h each → rows 0-6, 7-13
     *   left/right walls  (1,5,6): 14w × 17h each → rows 14-30 at x=0, x=14
     *   bottom            (6,1,6): 24w × 13h → row 31
     *   blood             (6,0,6): 24w × 12h → row 44  (top face only visible, 6×6 at u+6)
     *
     * Coord system (renderer applies: translate(0.5,0.5,0.5), rotateZ(180°), translate(0,-1,0)):
     *   Block x = 0.5 - model_x/16
     *   Block y = 1.5 - model_y/16
     *   Block z = 0.5 + model_z/16
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Front wall: block z=4..5, x=4..12, y=0..5
        // model x=-4..4 (8 wide), model y=19..24 (5 tall), model z=-4..-3 (1 deep)
        // world_z = 0.5 + model_z/16 → outer face at z=0.25 (=4/16), inner at z=0.3125 (=5/16)
        root.addOrReplaceChild("front_wall",
                CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 8, 5, 1),
                PartPose.offset(-4f, 19f, -4f));

        // Back wall: block z=11..12
        // model z=3..4
        root.addOrReplaceChild("back_wall",
                CubeListBuilder.create().texOffs(0, 7).addBox(0, 0, 0, 8, 5, 1),
                PartPose.offset(-4f, 19f, 3f));

        // Left wall: block x=4..5, z=5..11
        // model x=3..4 (1 wide), model z=-3..3 (6 deep)
        root.addOrReplaceChild("left_wall",
                CubeListBuilder.create().texOffs(0, 14).addBox(0, 0, 0, 1, 5, 6),
                PartPose.offset(3f, 19f, -3f));

        // Right wall: block x=11..12, z=5..11
        // model x=-4..-3
        root.addOrReplaceChild("right_wall",
                CubeListBuilder.create().texOffs(14, 14).addBox(0, 0, 0, 1, 5, 6),
                PartPose.offset(-4f, 19f, -3f));

        // Bottom: block x=5..11, y=0..1, z=5..11
        // model x=-3..3, y=23..24, z=-3..3
        root.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(0, 31).addBox(0, 0, 0, 6, 1, 6),
                PartPose.offset(-3f, 23f, -3f));

        // Blood liquid (flat): rendered at max-fill position, translated down by renderer for partial levels
        // model x=-3..3, y=20 (flat), z=-3..3
        root.addOrReplaceChild("blood",
                CubeListBuilder.create().texOffs(0, 44).addBox(0, 0, 0, 6, 0, 6),
                PartPose.offset(-3f, 20f, -3f));

        return LayerDefinition.create(mesh, 32, 64);
    }
}
