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
 * Worn Koboldite Helm / Twisting Band model, matching source {@code ItemGoblinClothes(0)}.
 *
 * <p>The old texture sheet only paints a thin circlet on the normal biped head box; the
 * inflated source armor scale makes it sit proud of the player head without becoming a
 * full helmet.
 */
public class TwistingBandArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "twisting_band_armor"), "main");

    public TwistingBandArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.61F), 0.0F);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
