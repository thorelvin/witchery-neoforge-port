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

/**
 * Source Witch Hunter clothing geometry: wide hat brim, stacked hat crown, and coat tails.
 * The model is returned through {@code HunterArmorClientExtensions}, which scopes visible
 * parts to the currently rendered armor slot.
 */
public class HunterArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hunter_armor"), "main");

    public HunterArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.4F), 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        CubeDeformation hatBrimDef = new CubeDeformation(0.32F);
        CubeDeformation hatCrownDef = new CubeDeformation(0.52F);
        PartDefinition hatBrim = head.addOrReplaceChild("hat_brim", CubeListBuilder.create().mirror()
                        .texOffs(0, 50).addBox(-6.5F, 0.0F, -6.5F, 13, 1, 13, hatBrimDef),
                PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition hatMid = hatBrim.addOrReplaceChild("hat_mid", CubeListBuilder.create().mirror()
                        .texOffs(40, 52).addBox(-4.0F, 0.0F, -4.0F, 8, 2, 8, hatCrownDef),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        hatMid.addOrReplaceChild("hat_top", CubeListBuilder.create().mirror()
                        .texOffs(12, 41).addBox(-3.5F, 0.0F, -3.5F, 7, 2, 7, hatCrownDef),
                PartPose.offset(0.0F, -2.0F, 0.0F));

        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("coat", CubeListBuilder.create().mirror()
                        .texOffs(41, 33).addBox(-5.5F, 0.0F, -3.0F, 11, 10, 6, new CubeDeformation(-0.3F)),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        addLegSlot(root, "right_leg", -1.9F, false);
        addLegSlot(root, "left_leg", 1.9F, true);

        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addLegSlot(PartDefinition root, String name, float pivotX, boolean mirror) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create(),
                PartPose.offset(pivotX, 12.0F, 0.0F));
        CubeDeformation slimArmor = new CubeDeformation(0.01F);
        CubeListBuilder legs = CubeListBuilder.create().texOffs(0, 16);
        CubeListBuilder boots = CubeListBuilder.create().texOffs(0, 16);
        if (mirror) {
            legs.mirror();
            boots.mirror();
        }
        leg.addOrReplaceChild("legs", legs.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, slimArmor), PartPose.ZERO);
        leg.addOrReplaceChild("boots", boots.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, slimArmor), PartPose.ZERO);
    }
}
