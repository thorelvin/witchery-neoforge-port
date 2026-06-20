package com.emoniph.witchery.client.model;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Custom worn-armor model for vampire clothing — the source/Resurrected look (top hat + caped coat,
 * veiled dress, or spiked chestplate) instead of the flat vanilla armor silhouette. Returned from the
 * vampire items' {@code IClientItemExtensions.getGenericArmorModel}; that hook copies the (animated)
 * pose from the original armor model onto the seven standard parts, then {@code VampireArmorClientExtensions}
 * hides every part and re-enables only the ones for the worn slot. Decorative cubes are parented to the
 * head / body / arms so they appear with the matching helmet / chestplate slot and inherit its pose.
 *
 * <p>Geometry/UVs ported cube-for-cube from Witchery Resurrected ({@code Masculine/Feminine/MetallicVampireArmor},
 * 128x64 textures). The 1.12 source re-parents the arms/legs under the body and nudges arm pivots by ±1 at
 * render; we instead keep the standard 1.21 part hierarchy and pivots (the pose copy overwrites arm pivots
 * anyway), translating each decorative cube into the standard part's local space. Head geometry is confirmed
 * in-game; chest/arms are this pass. Legs + feet remain plain (a later pass).
 */
public class VampireArmorModel extends HumanoidModel<HumanoidRenderState> {

    public enum Variant {
        MASCULINE("vampire_masculine", true),
        FEMININE("vampire_feminine", true),
        METALLIC("vampire_metallic", false);

        /** Default (wide-arm) baked layer. */
        public final ModelLayerLocation layer;
        /** Slim-arm baked layer, or {@link #layer} when this variant has no distinct slim model. */
        public final ModelLayerLocation slimLayer;
        /** Whether this variant ships a distinct slim-arm model + texture (source: masculine/feminine only). */
        public final boolean hasSlim;
        private final String texturePath;

        Variant(String path, boolean hasSlim) {
            this.texturePath = path;
            this.hasSlim = hasSlim;
            this.layer = new ModelLayerLocation(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path), "main");
            this.slimLayer = hasSlim
                    ? new ModelLayerLocation(
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path), "slim")
                    : this.layer;
        }

        /** Equipment texture id (humanoid layer); slim swaps to the {@code _slim} sheet. */
        public net.minecraft.resources.Identifier texture(boolean slim) {
            String path = (slim && hasSlim) ? texturePath + "_slim" : texturePath;
            return net.minecraft.resources.Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
        }
    }

    public VampireArmorModel(ModelPart root) {
        super(root);
    }

    /**
     * Build a leg part that carries two slot-scoped child groups: {@code "legs"} (trousers / leg-armor,
     * shown for the LEGS slot via the leggings texture) and {@code "boots"} (footwear, shown for the FEET
     * slot via the body texture). {@link com.emoniph.witchery.client.VampireArmorClientExtensions} toggles
     * which group is visible per worn slot. The boot cubes are inflated proud of the leg cubes so the two
     * never share a face (no z-fight when both pieces are worn).
     */
    private static void addLegSlot(PartDefinition root, String name, float pivotX,
                                   CubeListBuilder legs, CubeListBuilder boots) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create(),
                PartPose.offset(pivotX, 12.0F, 0.0F));
        leg.addOrReplaceChild("legs", legs, PartPose.ZERO);
        leg.addOrReplaceChild("boots", boots, PartPose.ZERO);
    }

    public static LayerDefinition createLayer(Variant variant, boolean slim) {
        CubeDeformation outer = new CubeDeformation(0.5F);
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");

        // The vampire headwear IS its own geometry on the head part. Each variant REPLACES the head
        // part below (dropping the standard 8x8x8 armour head box that would otherwise wrap the face
        // like a bucket); each replacement re-adds an empty "hat" child because HumanoidModel resolves
        // it via head.getChild("hat") — that empty child also removes the standard hat overlay box.

        switch (variant) {
            case MASCULINE -> {
                // --- Head: top hat (brim is the head part, crown is its child). ---
                PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(56, 13).addBox(-4.5F, -8.0F, -4.5F, 9, 1, 9, outer), PartPose.ZERO);
                head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
                head.addOrReplaceChild("crown", CubeListBuilder.create()
                        .texOffs(38, 0).addBox(-4.0F, -13.0F, -4.0F, 8, 5, 8, outer), PartPose.ZERO);

                // --- Body: collar, scarf (+ knot), cloak, coat-tails. ---
                body.addOrReplaceChild("collar", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -4.5F, 10, 4, 9, outer), PartPose.ZERO);
                PartDefinition scarf = body.addOrReplaceChild("scarf", CubeListBuilder.create()
                        .texOffs(29, 0).addBox(-1.5F, 2.0F, -1.5F, 3, 4, 2, CubeDeformation.NONE),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2618F, 0.0F, 0.0F));
                scarf.addOrReplaceChild("scarf_knot", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, 2.5F, -0.5F, 2, 3, 2, CubeDeformation.NONE),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
                body.addOrReplaceChild("cloak", CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-7.0F, 0.0F, 0.0F, 14, 21, 1, outer),
                        PartPose.offset(0.0F, 0.0F, 2.0F));
                // (The long coat-tails are the LEGS piece — texOffs 0,16/0,32, the source leg geometry —
                // not a body part. Building them on the body wrapped them down to the feet, where the body
                // sheet's boot UVs made them read as phantom shoes, and their top z-fought the waist.)

                // --- Arms: short cuffed sleeves (right UV 40,16 / left UV 40,29). ---
                int armW = slim ? 3 : 4;
                int cuffW = slim ? 4 : 5;
                PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16).addBox(slim ? -2.0F : -3.0F, -2.0F, -2.0F, armW, 9, 4, outer),
                        PartPose.offset(-5.0F, 2.0F, 0.0F));
                rightArm.addOrReplaceChild("cuff_right", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(slim ? -2.5F : -3.5F, 6.5F, -2.5F, cuffW, 3, 5, outer), PartPose.ZERO);
                PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(40, 29).addBox(-1.0F, -2.0F, -2.0F, armW, 9, 4, outer),
                        PartPose.offset(5.0F, 2.0F, 0.0F));
                leftArm.addOrReplaceChild("cuff_left", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-1.5F, 6.5F, -2.5F, cuffW, 3, 5, outer), PartPose.ZERO);

                // Trousers (LEGS) are the long coat-leg; tucked just under the coat torso (0.4 < body 0.5)
                // so the coat covers the waist seam without a coplanar z-fight. Tall boots (FEET) proud.
                CubeDeformation trouserDef = new CubeDeformation(0.4F);
                CubeDeformation mBootDef = new CubeDeformation(0.75F);
                addLegSlot(root, "right_leg", -1.9F,
                        CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, trouserDef),
                        CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, mBootDef));
                addLegSlot(root, "left_leg", 1.9F,
                        CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, trouserDef),
                        CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, mBootDef));
            }
            case FEMININE -> {
                // --- Head: hat band (head part) with veil, feather and a small top hat. ---
                PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(32, 6).addBox(-2.0F, -9.0F, -3.0F, 6, 1, 6, outer), PartPose.ZERO);
                head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
                head.addOrReplaceChild("veil", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -8.0F, -4.5F, 7, 8, 6, CubeDeformation.NONE), PartPose.ZERO);
                head.addOrReplaceChild("feather", CubeListBuilder.create()
                        .texOffs(20, 0).addBox(3.0F, -12.0F, 0.0F, 3, 3, 1, CubeDeformation.NONE),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1367F, 0.0F));
                head.addOrReplaceChild("veil_top_hat", CubeListBuilder.create()
                        .texOffs(38, 0).addBox(-1.0F, -11.0F, -2.0F, 4, 2, 4, outer), PartPose.ZERO);

                // --- Body: chest bodice + flared front/back skirt with layered side panels. ---
                body.addOrReplaceChild("bodice", CubeListBuilder.create()
                        .texOffs(20, 45).addBox(-3.5F, -0.8F, -6.5F, 7, 4, 4, outer),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));
                PartDefinition skirtFront = body.addOrReplaceChild("skirt_front", CubeListBuilder.create()
                        .texOffs(24, 32).addBox(-5.0F, 0.0F, 0.0F, 10, 11, 2, outer),
                        PartPose.offsetAndRotation(0.0F, 12.0F, -2.0F, -0.1222F, 0.0F, 0.0F));
                PartDefinition skirtBack = body.addOrReplaceChild("skirt_back", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-5.0F, 0.0F, -2.0F, 10, 11, 2, outer),
                        PartPose.offsetAndRotation(0.0F, 12.0F, 2.0F, 0.1222F, 0.0F, 0.0F));
                // Front side panels (right edge + mirrored left edge), slightly fanned to avoid z-fighting.
                skirtFront.addOrReplaceChild("skirt_f_mid_1", CubeListBuilder.create()
                        .texOffs(14, 45).addBox(-4.92F, 0.0F, 0.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtFront.addOrReplaceChild("skirt_f_mid_2", CubeListBuilder.create()
                        .texOffs(14, 45).addBox(-4.91F, 0.0F, 0.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtFront.addOrReplaceChild("skirt_f_mid_3", CubeListBuilder.create()
                        .texOffs(14, 45).addBox(-4.9F, 0.0F, 0.0F, 1, 11, 2, outer),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1222F, 0.0F, 0.0F));
                skirtFront.addOrReplaceChild("skirt_f_mid_l1", CubeListBuilder.create().mirror()
                        .texOffs(14, 45).addBox(3.92F, 0.0F, 0.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtFront.addOrReplaceChild("skirt_f_mid_l2", CubeListBuilder.create().mirror()
                        .texOffs(14, 45).addBox(3.91F, 0.0F, 0.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtFront.addOrReplaceChild("skirt_f_mid_l3", CubeListBuilder.create().mirror()
                        .texOffs(14, 45).addBox(3.9F, 0.0F, 0.0F, 1, 11, 2, outer),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1222F, 0.0F, 0.0F));
                // Back side panels.
                skirtBack.addOrReplaceChild("skirt_b_mid_1", CubeListBuilder.create()
                        .texOffs(14, 45).addBox(-4.92F, 0.0F, -2.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtBack.addOrReplaceChild("skirt_b_mid_2", CubeListBuilder.create()
                        .texOffs(14, 45).addBox(-4.91F, 0.0F, -2.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtBack.addOrReplaceChild("skirt_b_mid_3", CubeListBuilder.create()
                        .texOffs(14, 45).addBox(-4.9F, 0.0F, -2.0F, 1, 11, 2, outer),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1222F, 0.0F, 0.0F));
                skirtBack.addOrReplaceChild("skirt_b_mid_l1", CubeListBuilder.create().mirror()
                        .texOffs(14, 45).addBox(3.92F, 0.0F, -2.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtBack.addOrReplaceChild("skirt_b_mid_l2", CubeListBuilder.create().mirror()
                        .texOffs(14, 45).addBox(3.91F, 0.0F, -2.0F, 1, 11, 2, outer), PartPose.ZERO);
                skirtBack.addOrReplaceChild("skirt_b_mid_l3", CubeListBuilder.create().mirror()
                        .texOffs(14, 45).addBox(3.9F, 0.0F, -2.0F, 1, 11, 2, outer),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1222F, 0.0F, 0.0F));

                // --- Arms: full sleeves, no cuffs (right UV 40,16 / left UV 48,32). ---
                int armW = slim ? 3 : 4;
                root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16).addBox(slim ? -2.0F : -3.0F, -2.0F, -2.0F, armW, 12, 4, outer),
                        PartPose.offset(-5.0F, 2.0F, 0.0F));
                root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(48, 32).addBox(-1.0F, -2.0F, -2.0F, armW, 12, 4, outer),
                        PartPose.offset(5.0F, 2.0F, 0.0F));

                // The skirt (chest piece) covers the legs; the LEGS slot shows just the lower leg/ankle,
                // the FEET slot the heeled shoe (ankle + heel + toe), proud of the ankle.
                CubeDeformation ankleDef = new CubeDeformation(0.25F);
                CubeDeformation shoeDef = new CubeDeformation(0.5F);
                addLegSlot(root, "right_leg", -1.9F,
                        CubeListBuilder.create().texOffs(0, 20).addBox(-2.0F, 10.0F, -2.0F, 4, 2, 4, ankleDef),
                        CubeListBuilder.create()
                                .texOffs(0, 20).addBox(-2.0F, 10.0F, -2.0F, 4, 2, 4, shoeDef)
                                .texOffs(13, 16).addBox(-1.0F, 12.0F, 1.0F, 2, 2, 1, shoeDef)
                                .texOffs(0, 26).addBox(-2.0F, 12.0F, -3.0F, 4, 2, 3, shoeDef));
                addLegSlot(root, "left_leg", 1.9F,
                        CubeListBuilder.create().mirror().texOffs(0, 20).addBox(-2.0F, 10.0F, -2.0F, 4, 2, 4, ankleDef),
                        CubeListBuilder.create().mirror()
                                .texOffs(0, 20).addBox(-2.0F, 10.0F, -2.0F, 4, 2, 4, shoeDef)
                                .texOffs(13, 16).addBox(-1.0F, 12.0F, 1.0F, 2, 2, 1, shoeDef)
                                .texOffs(0, 26).addBox(-2.0F, 12.0F, -3.0F, 4, 2, 3, shoeDef));
            }
            case METALLIC -> {
                // --- Head: full helmet (head part) with collar and crown spikes. ---
                PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, outer), PartPose.ZERO);
                head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
                head.addOrReplaceChild("collar", CubeListBuilder.create()
                        .texOffs(64, 22).addBox(-5.0F, -4.0F, -4.5F, 10, 4, 9, outer), PartPose.ZERO);
                head.addOrReplaceChild("helmet_spikes", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.0F, -11.0F, -4.0F, 8, 3, 8, outer), PartPose.ZERO);

                // --- Body: front + back chest plates (with trim ridges) and a long cloak. ---
                // The main plates (y0..5) and their trim ridges (y5..6) abut at y=5; a uniform 0.5
                // inflation would grow each into a 1-unit overlapping band with coplanar faces (z-fight).
                // Grow these plates in x/z only (proud of the torso) but not y, so they stay flush at y=5.
                CubeDeformation plateDef = new CubeDeformation(0.5F, 0.0F, 0.5F);
                body.addOrReplaceChild("chestplate_front", CubeListBuilder.create()
                        .texOffs(20, 44).addBox(-4.0F, 0.0F, -3.0F, 8, 5, 1, plateDef), PartPose.ZERO);
                body.addOrReplaceChild("chestplate_front2", CubeListBuilder.create()
                        .texOffs(20, 50).addBox(-3.0F, 5.0F, -3.0F, 6, 1, 1, plateDef), PartPose.ZERO);
                body.addOrReplaceChild("chestplate_back", CubeListBuilder.create()
                        .texOffs(20, 44).addBox(-4.0F, 0.0F, 1.5F, 8, 5, 1, plateDef), PartPose.ZERO);
                body.addOrReplaceChild("chestplate_back2", CubeListBuilder.create()
                        .texOffs(20, 50).addBox(-3.0F, 5.0F, 1.5F, 6, 1, 1, plateDef), PartPose.ZERO);
                body.addOrReplaceChild("cloak", CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-7.0F, 0.0F, 0.0F, 14, 21, 1, outer),
                        PartPose.offset(0.0F, 0.0F, 2.0F));

                // --- Arms: gauntlets on both, a spiked pauldron on the right (metallic has no slim model). ---
                PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, outer),
                        PartPose.offset(-5.0F, 2.0F, 0.0F));
                // Inflate the gauntlet (source uses 0.0 here, but that makes its box exactly the arm's
                // deformed size — coplanar faces flicker on the right forearm; 0.5 sits it proud, matching
                // the left gauntlet).
                PartDefinition gauntletRight = rightArm.addOrReplaceChild("gauntlet_right", CubeListBuilder.create()
                        .texOffs(46, 43).addBox(-3.5F, 4.0F, -2.5F, 3, 5, 5, outer), PartPose.ZERO);
                gauntletRight.addOrReplaceChild("gauntlet_right_spikes", CubeListBuilder.create()
                        .texOffs(34, 39).addBox(-5.5F, 5.0F, 0.0F, 2, 3, 1, outer), PartPose.ZERO);
                PartDefinition pauldronRight = rightArm.addOrReplaceChild("pauldron_right", CubeListBuilder.create()
                        .texOffs(44, 32).addBox(-4.01F, -3.0F, -2.5F, 5, 6, 5, outer), PartPose.ZERO);
                pauldronRight.addOrReplaceChild("pauldron_spikes", CubeListBuilder.create()
                        .texOffs(20, 36).addBox(-4.01F, -6.0F, 0.0F, 3, 3, 1, outer), PartPose.ZERO);
                PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
                        .texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, outer),
                        PartPose.offset(5.0F, 2.0F, 0.0F));
                PartDefinition gauntletLeft = leftArm.addOrReplaceChild("gauntlet_left", CubeListBuilder.create().mirror()
                        .texOffs(46, 43).addBox(0.5F, 4.0F, -2.5F, 3, 5, 5, outer), PartPose.ZERO);
                gauntletLeft.addOrReplaceChild("gauntlet_left_spikes", CubeListBuilder.create().mirror()
                        .texOffs(34, 39).addBox(3.5F, 5.0F, 0.0F, 2, 3, 1, outer), PartPose.ZERO);

                // Leg-armor plates (LEGS), tucked under the chestplate body (0.4 < body 0.5) to keep the
                // waist seam from z-fighting; armored boots — sole + shin plate + sleeve — for FEET, the
                // sleeve proud (0.75) of the leg plate so the overlapping lower leg doesn't z-fight.
                CubeDeformation legPlateDef = new CubeDeformation(0.4F);
                CubeDeformation sleeveDef = new CubeDeformation(0.75F);
                addLegSlot(root, "right_leg", -1.9F,
                        CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 10, 4, legPlateDef),
                        CubeListBuilder.create()
                                .texOffs(0, 54).addBox(-2.0F, 10.0F, -4.0F, 4, 2, 6, outer)
                                .texOffs(0, 44).addBox(-2.0F, 2.0F, -3.0F, 4, 8, 1, outer)
                                .texOffs(0, 32).addBox(-2.0F, 3.0F, -2.0F, 4, 7, 4, sleeveDef));
                addLegSlot(root, "left_leg", 1.9F,
                        CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 10, 4, legPlateDef),
                        CubeListBuilder.create().mirror()
                                .texOffs(0, 54).addBox(-2.0F, 10.0F, -4.0F, 4, 2, 6, outer)
                                .texOffs(0, 44).addBox(-2.0F, 2.0F, -3.0F, 4, 8, 1, outer)
                                .texOffs(0, 32).addBox(-2.0F, 3.0F, -2.0F, 4, 7, 4, sleeveDef));
            }
        }

        return LayerDefinition.create(mesh, 128, 64);
    }
}
