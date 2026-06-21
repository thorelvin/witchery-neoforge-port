package com.emoniph.witchery.client.model;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Source-style Witchery footwear model from {@code ModelWitchesClothes(0.45F, false)}.
 */
public class WitchesShoesArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witches_shoes_armor"), "main");

    public WitchesShoesArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.45F), 0.0F);
        return LayerDefinition.create(mesh, 128, 64);
    }
}
