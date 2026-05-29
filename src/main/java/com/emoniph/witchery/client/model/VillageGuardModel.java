package com.emoniph.witchery.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Village Guard model. 64×64 texture, villager-proportion head (8×10×8).
 *
 * ARMOR_HEAD_LAYER is a head-only partial armor model used for the HEAD slot in
 * HumanoidArmorLayer. Chest/legs/feet slots use standard zombie armor models.
 * The head part is shifted up by 0.5 via PartPose so that CubeDeformation(0.5f)
 * inflates the top/sides outward but the bottom lands exactly at y=0 (jaw level)
 * instead of y=+0.5, preventing the grey bottom-face artefact below the chin.
 */
public class VillageGuardModel<S extends HumanoidRenderState> extends HumanoidModel<S> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "village_guard"), "main");

    /** HEAD-slot armor model only — all other parts are empty stubs. */
    public static final ModelLayerLocation ARMOR_HEAD_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "village_guard"), "armor_head");

    public VillageGuardModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    // ── Skin model ────────────────────────────────────────────────────────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(28, 46).addBox(-4, -10, -4, 8, 10, 8),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -4, 8, 10, 8, new CubeDeformation(0.5f)),
                PartPose.ZERO);
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(52, 46).addBox(-1, -1, -6, 2, 4, 2),
                PartPose.offset(0, -2, 0));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.offset(0, 0, 0));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4),
                PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4),
                PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(2, 12, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ── Armor head model ─────────────────────────────────────────────────────

    /**
     * Head-only armor layer used for the HEAD slot in HumanoidArmorLayer.
     * All non-head parts are empty so they render nothing (chest/legs/feet
     * slots use the standard zombie armor models instead).
     *
     * The head part is at PartPose.offset(0, -0.5f, 0) — shifted 0.5 units
     * upward. CubeDeformation(0.5f) then inflates the box symmetrically, but
     * because the origin is raised by 0.5 the inflated bottom lands exactly at
     * y=0 (jaw level) rather than y=+0.5. This eliminates the grey bottom-face
     * square that was visible at the chin from low/diagonal view angles.
     */
    public static LayerDefinition createArmorHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Vanilla ZombieVillagerModel.createBaseArmorMesh uses addBox(-4,-10,-4,8,8,8)
        // — 8 units tall, NOT 10. The helmet covers only the top of the head
        // (y=-10.5 to y=-1.5 after CubeDeformation(0.5f)), leaving the face area
        // (y=-2 to y=0) fully uncovered. This eliminates the grey wrapping the face.
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4, -10, -4, 8, 8, 8, new CubeDeformation(0.5f)),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        // Required stubs — no geometry, render nothing
        root.addOrReplaceChild("body",      CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm",  CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg",  CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
