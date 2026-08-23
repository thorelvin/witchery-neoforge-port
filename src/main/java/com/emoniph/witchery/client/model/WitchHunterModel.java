package com.emoniph.witchery.client.model;

import com.emoniph.witchery.client.renderer.WitchHunterRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Source-exact geometry extension of the Minecraft 1.7.10 biped model. */
public class WitchHunterModel extends LegacyBipedModel<WitchHunterRenderer.WitchHunterRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "witch_hunter"), "main");

    public WitchHunterModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = LegacyBipedModel.createLegacyMesh();
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("coat_bottom",
                CubeListBuilder.create().texOffs(1, 47)
                        .addBox(-5.0F, 0.0F, -2.5F, 10.0F, 11.0F, 5.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("hat_brim",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-7.0F, 0.0F, -7.0F, 14.0F, 1.0F, 14.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));
        head.addOrReplaceChild("hat_top",
                CubeListBuilder.create().texOffs(33, 48)
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, -10.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
