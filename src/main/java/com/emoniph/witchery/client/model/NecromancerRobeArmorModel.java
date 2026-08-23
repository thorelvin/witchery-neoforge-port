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
 * Source-style Necromancer Robes model from {@code ModelWitchesClothes(0.61F, true)}.
 */
public class NecromancerRobeArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "necromancer_robe_armor"), "main");

    public NecromancerRobeArmorModel(ModelPart root) {
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

        CubeDeformation shoulderDeformation = new CubeDeformation(sourceScale + 0.1F);
        root.getChild("right_arm").addOrReplaceChild("shoulder_plate", CubeListBuilder.create()
                        .texOffs(61, 32).addBox(0.0F, 0.0F, 0.0F, 5, 1, 6, shoulderDeformation),
                PartPose.offset(-4.0F, -2.0F, -3.0F));
        root.getChild("left_arm").addOrReplaceChild("shoulder_plate", CubeListBuilder.create()
                        .texOffs(61, 39).addBox(0.0F, 0.0F, 0.0F, 5, 1, 6, shoulderDeformation),
                PartPose.offset(0.0F, -2.0F, -3.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
