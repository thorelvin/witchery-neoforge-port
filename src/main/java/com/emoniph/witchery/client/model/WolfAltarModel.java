package com.emoniph.witchery.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Wolf-god statue for the Wolf Altar, ported 1:1 from the decompiled {@code ModelWolfAltar} (texture
 * 128×128, {@code wolfaltar.png}): a robed upright wolfman holding a staff atop a stepped plinth, with
 * two smaller wolves flanking the base. The source render scales the sub-figures independently
 * (biped ×0.7, each wolf ×0.5), which {@link com.emoniph.witchery.client.renderer.WolfAltarRenderer}
 * reproduces by submitting the top-level parts separately — hence the accessors below.
 */
public class WolfAltarModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "wolf_altar"), "main");

    private final ModelPart root;
    private final ModelPart bipedBody;
    private final ModelPart wolfBody;
    private final ModelPart wolfBody1;
    private final ModelPart shape37;
    private final ModelPart shape1;
    private final ModelPart shape1_1;
    private final ModelPart shape1_2;

    public WolfAltarModel(ModelPart root) {
        this.root = root;
        this.bipedBody = root.getChild("biped_body");
        this.wolfBody = root.getChild("wolf_body");
        this.wolfBody1 = root.getChild("wolf_body_1");
        this.shape37 = root.getChild("shape37");
        this.shape1 = root.getChild("shape1");
        this.shape1_1 = root.getChild("shape1_1");
        this.shape1_2 = root.getChild("shape1_2");
    }

    public ModelPart bipedBody() { return bipedBody; }
    public ModelPart wolfBody() { return wolfBody; }
    public ModelPart wolfBody1() { return wolfBody1; }
    public ModelPart shape37() { return shape37; }
    public ModelPart shape1() { return shape1; }
    public ModelPart shape1_1() { return shape1_1; }
    public ModelPart shape1_2() { return shape1_2; }

    /** Apply a uniform scale about a part's (pixel) pivot, mirroring the source per-subtree GL scaling. */
    public static void scaleAboutPivot(PoseStack ps, float px, float py, float pz, float scale) {
        ps.translate(px / 16.0F, py / 16.0F, pz / 16.0F);
        ps.scale(scale, scale, scale);
        ps.translate(-px / 16.0F, -py / 16.0F, -pz / 16.0F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Upright wolfman figure (rendered ×0.7) ──────────────────────────────
        PartDefinition body = root.addOrReplaceChild("biped_body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4),
                PartPose.offset(0.0F, 3.1F, 0.0F));
        body.addOrReplaceChild("biped_left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(0.0F, 12.0F, -2.0F, 4, 12, 4),
                PartPose.ZERO);
        PartDefinition rightLeg = body.addOrReplaceChild("biped_right_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4.0F, 7.4F, 8.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) (-Math.PI / 3), 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("biped_right_leg_lower",
                CubeListBuilder.create().texOffs(18, 0).addBox(-4.0F, 15.7F, -6.2F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.95609134F, 0.0F, 0.0F));
        body.addOrReplaceChild("biped_left_arm",
                CubeListBuilder.create().mirror().texOffs(40, 16).addBox(4.0F, -2.0F, -2.0F, 4, 12, 4),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.4553564F, -0.13665928F, 0.0F));
        PartDefinition rightArm = body.addOrReplaceChild("biped_right_arm",
                CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-8.0F, -2.0F, -2.0F, 4, 12, 4),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -2.4586453F, 0.4098033F, 0.0F));
        rightArm.addOrReplaceChild("shaft",
                CubeListBuilder.create().texOffs(0, 33).addBox(-6.5F, 8.0F, -17.9F, 1, 1, 40),
                PartPose.ZERO);
        PartDefinition bipedWolfHead = body.addOrReplaceChild("wolf_head_main0",
                CubeListBuilder.create().texOffs(0, 42).addBox(-3.0F, -6.0F, -2.0F, 6, 6, 5),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.13665928F, 0.13665928F, 0.0F));
        bipedWolfHead.addOrReplaceChild("wolf_head_main3",
                CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -3.1F, -5.0F, 3, 3, 4), PartPose.ZERO);
        bipedWolfHead.addOrReplaceChild("wolf_head_main2",
                CubeListBuilder.create().texOffs(16, 14).addBox(1.0F, -8.0F, 2.0F, 2, 2, 1), PartPose.ZERO);
        bipedWolfHead.addOrReplaceChild("wolf_head_main1",
                CubeListBuilder.create().texOffs(16, 14).addBox(-3.0F, -8.0F, 2.0F, 2, 2, 1), PartPose.ZERO);

        // ── Stepped plinth (rendered ×1) ────────────────────────────────────────
        root.addOrReplaceChild("shape37",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 4, 2, 4),
                PartPose.offset(-3.9F, 18.0F, -6.5F));
        root.addOrReplaceChild("shape1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 16, 2, 16),
                PartPose.offset(-8.0F, 22.0F, -8.0F));
        root.addOrReplaceChild("shape1_1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 14, 1, 14),
                PartPose.offset(-7.0F, 21.0F, -7.0F));
        root.addOrReplaceChild("shape1_2",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 16, 1, 16),
                PartPose.offset(-8.0F, 20.0F, -8.0F));

        // ── First flanking wolf (rendered ×0.5) ─────────────────────────────────
        addWolf(root, "wolf_body", -5.0F, 14.0F, 3.5F, 0.27314404F, true);
        // ── Second flanking wolf (rendered ×0.5) ────────────────────────────────
        addWolf(root, "wolf_body_1", 5.5F, 14.0F, -2.0F, 0.31869712F, false);

        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * Adds one of the two small flanking wolves. The first wolf carries small rotations on its head and
     * a couple of legs (source); the second is the un-rotated variant. Geometry is identical otherwise.
     */
    private static void addWolf(PartDefinition root, String name, float px, float py, float pz,
                                float bodyYaw, boolean first) {
        PartDefinition wolf = root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6, 6, 9),
                PartPose.offsetAndRotation(px, py, pz, 0.0F, bodyYaw, 0.0F));

        PartDefinition head = wolf.addOrReplaceChild("wolf_head_main0",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.5F, -13.0F, 6, 6, 4),
                first ? PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.22759093F, -0.22759093F, 0.0F)
                      : PartPose.ZERO);
        head.addOrReplaceChild("wolf_head_main1",
                CubeListBuilder.create().texOffs(16, 14).addBox(-3.0F, -4.5F, -10.0F, 2, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("wolf_head_main2",
                CubeListBuilder.create().texOffs(16, 14).addBox(1.0F, -4.5F, -10.0F, 2, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("wolf_head_main3",
                CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, 0.5F, -17.1F, 3, 3, 4), PartPose.ZERO);

        wolf.addOrReplaceChild("wolf_leg1",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.5F, 4.0F, 3.0F, 2, 8, 2), PartPose.ZERO);
        wolf.addOrReplaceChild("wolf_leg2",
                CubeListBuilder.create().texOffs(0, 18).addBox(0.5F, 4.0F, 3.0F, 2, 8, 2), PartPose.ZERO);
        wolf.addOrReplaceChild("wolf_leg3",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.5F, 4.0F, -8.0F, 2, 8, 2),
                first ? PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.091106184F, 0.0F) : PartPose.ZERO);
        wolf.addOrReplaceChild("wolf_leg4",
                CubeListBuilder.create().texOffs(0, 18).addBox(0.5F, 4.0F, -8.0F, 2, 8, 2),
                first ? PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.091106184F, 0.0F) : PartPose.ZERO);
        wolf.addOrReplaceChild("wolf_tail",
                CubeListBuilder.create().texOffs(9, 18).addBox(-1.0F, 0.0F, 4.0F, 2, 8, 2),
                PartPose.offsetAndRotation(0.0F, 0.6F, 0.0F, 0.31869712F, 0.0F, 0.0F));
        wolf.addOrReplaceChild("wolf_mane",
                CubeListBuilder.create().texOffs(21, 0).addBox(-4.0F, -3.0F, -9.0F, 8, 7, 6),
                first ? PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.13665928F, 0.0F) : PartPose.ZERO);
    }
}
