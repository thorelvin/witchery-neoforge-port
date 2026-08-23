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
 * Worn Baba Yaga's Hat model, ported from source {@code ModelWitchesClothes}.
 *
 * <p>The source keeps the standard biped head texture transparent and swaps the normal
 * witch hat mesh for this crooked hat stack whenever the head item is Baba's Hat.
 */
public class BabasHatArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "babas_hat_armor"), "main");

    public BabasHatArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.61F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");

        PartDefinition brim = head.addOrReplaceChild("babas_brim", CubeListBuilder.create()
                        .texOffs(72, 48).addBox(0.0F, 0.0F, 0.0F, 14, 2, 14, new CubeDeformation(0.52F)),
                PartPose.offset(-7.0F, -8.0F, -7.0F));
        PartDefinition crown = brim.addOrReplaceChild("babas_crown", CubeListBuilder.create()
                        .texOffs(83, 29).addBox(0.0F, 0.0F, 0.0F, 7, 4, 7, new CubeDeformation(0.4F)),
                PartPose.offsetAndRotation(3.75F, -4.0F, 4.0F,
                        -0.05235988F, 0.0F, 0.02617994F));
        PartDefinition upper = crown.addOrReplaceChild("babas_upper", CubeListBuilder.create()
                        .texOffs(83, 40).addBox(0.0F, 0.0F, 0.0F, 4, 4, 4, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.75F, -4.0F, 2.0F,
                        -0.10471976F, 0.0F, 0.05235988F));
        upper.addOrReplaceChild("babas_point", CubeListBuilder.create()
                        .texOffs(81, 48).addBox(0.0F, 0.0F, 0.0F, 1, 2, 1, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(1.75F, -2.0F, 2.0F,
                        (float) (-Math.PI / 15.0D), 0.0F, 0.10471976F));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
