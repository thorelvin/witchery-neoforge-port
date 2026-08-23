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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/** Exact wearable geometry and pose logic from Witchery 0.24.1 {@code ModelVampireArmor}. */
public class VampireArmorModel extends HumanoidModel<HumanoidRenderState> {
    public enum Variant {
        MASCULINE("vampire_masculine", false, false),
        FEMININE("vampire_feminine", true, false),
        METALLIC("vampire_metallic", false, true),
        METALLIC_FEMININE("vampire_metallic_feminine", true, true);

        public final ModelLayerLocation layer;
        public final boolean female;
        public final boolean metal;

        Variant(String path, boolean female, boolean metal) {
            this.layer = new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path), "main");
            this.female = female;
            this.metal = metal;
        }
    }

    private final Variant variant;
    private final ModelPart outerBody;
    private final ModelPart innerBody;
    private final ModelPart rightLeggings;
    private final ModelPart leftLeggings;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;
    private final ModelPart skirtBack;
    private final ModelPart skirtFront;
    private final ModelPart skirtMiddle;
    private final ModelPart skirtMiddle2;
    private final ModelPart skirtMiddle3;
    private final ModelPart cloakLeft;
    private final ModelPart cloakRight;
    private final ModelPart cloakMain;
    private final ModelPart sourceHat;
    private final ModelPart sourceHatBrim;
    private final ModelPart chest;
    private boolean sourceRiding;
    private boolean sourceCrouching;

    public VampireArmorModel(ModelPart root, Variant variant) {
        super(root);
        this.variant = variant;
        this.outerBody = this.body.getChild("outer_body");
        this.innerBody = this.body.getChild("inner_body");
        this.rightLeggings = this.rightLeg.getChild("leggings");
        this.leftLeggings = this.leftLeg.getChild("leggings");
        this.rightBoot = this.rightLeg.getChild("boot");
        this.leftBoot = this.leftLeg.getChild("boot");
        this.skirtBack = root.getChild("skirt_back");
        this.skirtFront = root.getChild("skirt_front");
        this.skirtMiddle = root.getChild("skirt_middle");
        this.skirtMiddle2 = root.getChild("skirt_middle2");
        this.skirtMiddle3 = root.getChild("skirt_middle3");
        this.cloakLeft = root.getChild("cloak_left");
        this.cloakRight = root.getChild("cloak_right");
        this.cloakMain = root.getChild("cloak_main");
        this.sourceHat = this.head.getChild("source_hat");
        this.sourceHatBrim = this.head.getChild("source_hat_brim");
        this.chest = root.getChild("chest");
        hideCustomParts();
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.3F), 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("outer_body", CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(0.3F)), PartPose.ZERO);
        body.addOrReplaceChild("inner_body", CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(0.02F)), PartPose.ZERO);

        addLeg(root, "right_leg", -1.9F, false);
        addLeg(root, "left_leg", 1.9F, true);

        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("source_hat", CubeListBuilder.create().texOffs(0, 67)
                        .addBox(-4, -15, -4, 8, 8, 8, new CubeDeformation(0.6F)),
                PartPose.offset(0, -0.5F, 0));
        head.addOrReplaceChild("source_hat_brim", CubeListBuilder.create().texOffs(0, 85)
                        .addBox(-5, -7, -5, 10, 1, 10, new CubeDeformation(0.7F)), PartPose.ZERO);

        root.addOrReplaceChild("skirt_back", CubeListBuilder.create().texOffs(26, 32)
                        .addBox(-4.5F, 0, -2.5F, 9, 12, 5), PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("skirt_front", CubeListBuilder.create().texOffs(26, 50)
                        .addBox(-4.5F, 0, -2.5F, 9, 12, 5), PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("skirt_middle", CubeListBuilder.create().texOffs(26, 68)
                        .addBox(-4.5F, 0, -2.5F, 9, 12, 5), PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("skirt_middle2", CubeListBuilder.create().texOffs(26, 68)
                        .addBox(-4.5F, 0, -2.5F, 9, 12, 5), PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("skirt_middle3", CubeListBuilder.create().texOffs(26, 68)
                        .addBox(-4.5F, 0, -2.5F, 9, 12, 5), PartPose.offset(0, 11, 0));

        root.addOrReplaceChild("cloak_left", CubeListBuilder.create().texOffs(0, 56)
                        .addBox(-3.5F, -8, 4, 7, 7, 1),
                PartPose.rotation((float) (-Math.PI / 9.0), 0.5108652F, 0.41086525F));
        root.addOrReplaceChild("cloak_right", CubeListBuilder.create().texOffs(0, 56)
                        .addBox(-3.5F, -8, 4, 7, 7, 1),
                PartPose.rotation((float) (-Math.PI / 9.0), -0.5108652F, -0.41086525F));
        root.addOrReplaceChild("cloak_main", CubeListBuilder.create().texOffs(0, 33)
                        .addBox(-6, 0, 2.5F, 12, 22, 1),
                PartPose.offsetAndRotation(0, 1, 0, 0.045553092F, 0, 0));
        root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(16, 67)
                        .addBox(-4, -2, -5, 8, 4, 4),
                PartPose.offsetAndRotation(0, 2, 0, (float) (Math.PI / 4.0), 0, 0));

        return LayerDefinition.create(mesh, 64, 96);
    }

    private static void addLeg(PartDefinition root, String name, float x, boolean mirror) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, 12, 0));
        CubeListBuilder leggings = CubeListBuilder.create().texOffs(0, 16);
        CubeListBuilder boot = CubeListBuilder.create().texOffs(0, 16);
        if (mirror) {
            leggings.mirror();
            boot.mirror();
        }
        leg.addOrReplaceChild("leggings", leggings.addBox(-2, 0, -2, 4, 12, 4,
                new CubeDeformation(0.02F)), PartPose.ZERO);
        leg.addOrReplaceChild("boot", boot.addBox(-2, 0, -2, 4, 12, 4,
                new CubeDeformation(0.3F)), PartPose.ZERO);
    }

    public void syncSourcePose(HumanoidModel<?> original) {
        this.sourceCrouching = Math.abs(this.body.xRot - 0.5F) < 0.12F;
        this.sourceRiding = isSourceRidingPose();

        float maximum = Math.max(this.rightLeg.xRot, this.leftLeg.xRot);
        float minimum = Math.min(this.rightLeg.xRot, this.leftLeg.xRot);
        this.skirtBack.xRot = maximum;
        this.skirtMiddle.xRot = maximum * 0.5F;
        this.skirtFront.xRot = minimum;
        this.skirtMiddle2.xRot = minimum * 0.5F;
        this.skirtMiddle3.xRot = 0.0F;

        float skirtY = this.sourceCrouching ? 8.0F : 11.0F;
        float skirtZ = this.sourceCrouching ? 4.0F : 0.0F;
        for (ModelPart part : new ModelPart[] {
                skirtBack, skirtFront, skirtMiddle, skirtMiddle2, skirtMiddle3}) {
            part.y = skirtY;
            part.z = skirtZ;
        }

        float movement = Math.max(Math.abs(this.rightLeg.xRot), Math.abs(this.leftLeg.xRot)) / 1.4F;
        this.cloakMain.xRot = this.sourceCrouching ? 0.6F : 0.045553092F;
        if (!this.sourceCrouching && movement > 0.1F) {
            this.cloakMain.xRot += movement * 0.8F - 0.1F;
        }
        float sideCloakPitch = this.head.xRot < -0.15F ? this.head.xRot - 0.15F : -0.3F;
        this.cloakLeft.xRot = sideCloakPitch;
        this.cloakRight.xRot = sideCloakPitch;
        this.sourceHat.y = -0.5F;
    }

    public void applySlotVisibility(EquipmentSlot slot) {
        this.setAllVisible(false);
        hideCustomParts();
        switch (slot) {
            case HEAD -> {
                this.head.visible = true;
                this.hat.visible = this.variant.metal;
                this.sourceHat.visible = !this.variant.metal;
                this.sourceHatBrim.visible = !this.variant.metal;
            }
            case CHEST -> {
                this.body.visible = true;
                this.rightArm.visible = true;
                this.leftArm.visible = true;
                this.outerBody.visible = true;
                this.cloakLeft.visible = true;
                this.cloakRight.visible = true;
                this.cloakMain.visible = true;
                this.chest.visible = this.variant.female;
            }
            case LEGS -> {
                this.body.visible = true;
                this.rightLeg.visible = true;
                this.leftLeg.visible = true;
                this.innerBody.visible = true;
                this.rightLeggings.visible = true;
                this.leftLeggings.visible = true;
                boolean showSkirt = this.variant.female && !this.sourceRiding;
                this.skirtBack.visible = showSkirt;
                this.skirtFront.visible = showSkirt;
                this.skirtMiddle.visible = showSkirt;
                this.skirtMiddle2.visible = showSkirt;
                this.skirtMiddle3.visible = showSkirt;
            }
            case FEET -> {
                this.rightLeg.visible = true;
                this.leftLeg.visible = true;
                this.rightBoot.visible = true;
                this.leftBoot.visible = true;
            }
            default -> {
            }
        }
    }

    private boolean isSourceRidingPose() {
        return Math.abs(this.rightLeg.xRot + 1.4137167F) < 0.08F
                && Math.abs(this.leftLeg.xRot + 1.4137167F) < 0.08F;
    }

    private void hideCustomParts() {
        this.outerBody.visible = false;
        this.innerBody.visible = false;
        this.rightLeggings.visible = false;
        this.leftLeggings.visible = false;
        this.rightBoot.visible = false;
        this.leftBoot.visible = false;
        this.sourceHat.visible = false;
        this.sourceHatBrim.visible = false;
        this.skirtBack.visible = false;
        this.skirtFront.visible = false;
        this.skirtMiddle.visible = false;
        this.skirtMiddle2.visible = false;
        this.skirtMiddle3.visible = false;
        this.cloakLeft.visible = false;
        this.cloakRight.visible = false;
        this.cloakMain.visible = false;
        this.chest.visible = false;
    }

    public ModelPart chainBody() {
        return this.outerBody;
    }

    public ModelPart chainChest() {
        return this.chest;
    }
}
