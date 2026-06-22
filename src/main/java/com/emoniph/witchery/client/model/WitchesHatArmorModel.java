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
 * Worn Witches' Hat model, ported from source {@code ModelWitchesClothes}.
 */
public class WitchesHatArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witches_hat_armor"), "main");

    public WitchesHatArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("witches_hat", CubeListBuilder.create().mirror()
                        .texOffs(0, 49).addBox(-7.0F, -7.0F, -7.0F, 14, 1, 14)
                        .texOffs(0, 36).addBox(-5.0F, -9.0F, -5.0F, 10, 2, 10)
                        .texOffs(31, 34).addBox(-3.0F, -14.0F, -3.0F, 6, 5, 6)
                        .texOffs(50, 34).addBox(-1.0F, -17.0F, -1.0F, 2, 3, 2),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }
}
