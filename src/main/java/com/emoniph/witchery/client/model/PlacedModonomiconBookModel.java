package com.emoniph.witchery.client.model;

import java.util.Set;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class PlacedModonomiconBookModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "placed_modonomicon_book"), "main");
    private static final float COVER_THICKNESS = 1.0F;
    private static final float COVER_FACE_THICKNESS = 0.005F;
    private static final float COVER_EDGE_THICKNESS = COVER_THICKNESS - COVER_FACE_THICKNESS;
    private static final float VISUAL_RIGHT_COVER_DEPTH_OFFSET = 1.0F;
    private static final Set<Direction> VISUAL_RIGHT_COVER_EDGE_FACES = Set.of(
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST);
    private static final Set<Direction> VISUAL_LEFT_COVER_EDGE_FACES = Set.of(
            Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.EAST, Direction.WEST);
    private static final Set<Direction> SPINE_EDGE_FACES = Set.of(
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private final ModelPart visualRightCover;
    private final ModelPart visualLeftCover;
    private final ModelPart visualRightPages;
    private final ModelPart visualLeftPages;
    private final ModelPart flipPage1;
    private final ModelPart flipPage2;

    public PlacedModonomiconBookModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.visualRightCover = root.getChild("visual_right_cover");
        this.visualLeftCover = root.getChild("visual_left_cover");
        this.visualRightPages = root.getChild("visual_right_pages");
        this.visualLeftPages = root.getChild("visual_left_pages");
        this.flipPage1 = root.getChild("flip_page1");
        this.flipPage2 = root.getChild("flip_page2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Opened-view axis probe for this visual-right cover:
        // +X appears left, +Y appears up, +Z pushes deeper into the book/back cover.
        root.addOrReplaceChild("visual_right_cover",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6.0F,
                                -5.0F,
                                -COVER_FACE_THICKNESS + VISUAL_RIGHT_COVER_DEPTH_OFFSET,
                                6.0F,
                                10.0F,
                                COVER_FACE_THICKNESS)
                        .texOffs(32, 19)
                        .addBox(-6.0F,
                                -5.0F,
                                -COVER_THICKNESS + VISUAL_RIGHT_COVER_DEPTH_OFFSET,
                                6.0F,
                                10.0F,
                                COVER_EDGE_THICKNESS,
                                VISUAL_RIGHT_COVER_EDGE_FACES),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        root.addOrReplaceChild("visual_left_cover",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(0.0F, -5.0F, -COVER_FACE_THICKNESS, 6.0F, 10.0F, COVER_FACE_THICKNESS)
                        .texOffs(32, 19)
                        .addBox(0.0F, -5.0F, 0.0F, 6.0F, 10.0F, COVER_EDGE_THICKNESS, VISUAL_LEFT_COVER_EDGE_FACES),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        root.addOrReplaceChild("seam",
                CubeListBuilder.create()
                        .texOffs(12, 0)
                        .addBox(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, COVER_FACE_THICKNESS)
                        .texOffs(48, 19)
                        .addBox(-1.0F, -5.0F, -COVER_THICKNESS, 2.0F, 10.0F, COVER_EDGE_THICKNESS, SPINE_EDGE_FACES),
                PartPose.rotation(0.0F, (float) (Math.PI / 2.0D), 0.0F));
        root.addOrReplaceChild("visual_right_pages",
                CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, -4.0F, -0.99F, 5.0F, 8.0F, 1.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("visual_left_pages",
                CubeListBuilder.create().texOffs(12, 10).addBox(0.0F, -4.0F, -0.01F, 5.0F, 8.0F, 1.0F),
                PartPose.ZERO);
        CubeListBuilder flipPage = CubeListBuilder.create().texOffs(24, 10).addBox(0.0F, -4.0F, 0.0F, 5.0F, 8.0F, 0.005F);
        root.addOrReplaceChild("flip_page1", flipPage, PartPose.ZERO);
        root.addOrReplaceChild("flip_page2", flipPage, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public void setupAnim(float time, float rightPageFlipAmount, float leftPageFlipAmount, float openAmount) {
        float easedOpen = Mth.clamp(openAmount, 0.0F, 1.0F);
        float spread = 1.2F * easedOpen;
        this.visualRightCover.yRot = (float) Math.PI + spread;
        this.visualLeftCover.yRot = -spread;
        this.visualRightPages.yRot = spread;
        this.visualLeftPages.yRot = -spread;
        this.flipPage1.yRot = spread - spread * 2.0F * rightPageFlipAmount;
        this.flipPage2.yRot = -spread + spread * 2.0F * leftPageFlipAmount;
        float hingeOffset = Mth.sin(spread);
        this.visualRightPages.x = hingeOffset;
        this.visualLeftPages.x = hingeOffset;
        this.flipPage1.x = hingeOffset;
        this.flipPage2.x = hingeOffset;
    }

    public void setFlipPagesVisible(boolean pageOneVisible, boolean pageTwoVisible) {
        this.flipPage1.visible = pageOneVisible;
        this.flipPage2.visible = pageTwoVisible;
    }
}
