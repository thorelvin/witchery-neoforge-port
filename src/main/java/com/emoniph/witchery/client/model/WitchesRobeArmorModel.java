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
 * Worn model for Witches' Robes, ported from the robe-owned pieces of source
 * {@code ModelWitchesClothes(0.61F, false)}.
 */
public class WitchesRobeArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witches_robe_armor"), "main");

    public WitchesRobeArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        float sourceScale = 0.61F;
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(sourceScale), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");

        body.addOrReplaceChild("lower_torso", CubeListBuilder.create()
                        .texOffs(43, 46).addBox(-4.0F, 0.0F, -2.0F, 8, 6, 4,
                                new CubeDeformation(sourceScale)),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
