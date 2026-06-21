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
 * Worn Death clothing model, ported from source {@code ModelClothesDeath}.
 *
 * <p>The old set uses a single 128x64 {@code deathsclothes} sheet: the hood owns the cowl,
 * the robe owns the body/sleeves plus the long robe cube, and Death's Footwear owns both
 * leg parts because there is no separate Death leggings item.
 */
public class DeathArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "death_armor"), "main");

    public DeathArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("cowl", CubeListBuilder.create()
                        .texOffs(0, 45).addBox(-4.5F, -8.5F, -4.5F, 9, 10, 9, new CubeDeformation(0.4F)),
                PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(72, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("robe", CubeListBuilder.create()
                        .texOffs(36, 37).addBox(-4.5F, 0.0F, -2.5F, 9, 22, 5, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(96, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.05F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("sleeve", CubeListBuilder.create()
                        .texOffs(64, 50).addBox(-3.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(96, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.05F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("sleeve", CubeListBuilder.create()
                        .texOffs(64, 50).addBox(-1.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(56, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.05F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(56, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.05F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
