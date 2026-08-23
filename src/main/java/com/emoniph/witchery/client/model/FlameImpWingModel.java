package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class FlameImpWingModel extends EntityModel<FlameImpModel.FlameImpRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "flame_imp_wings"), "main");

    public FlameImpWingModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(23, 21).addBox(0, 0, 0, 14, 21, 0),
                PartPose.offsetAndRotation(-2, 10, -1, 0.3047198F, -0.6698132F, -0.6283185F));
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(23, 21).addBox(0, 0, 0, 14, 21, 0),
                PartPose.offsetAndRotation(2, 10, -1, -0.3047198F, 3.811406F, 0.6283185F));
        // Source addBox() baked these UVs while the renderer still used ModelImp's 64 x 64 size.
        // Its later setTextureSize(128, 32) call did not retroactively update the ModelBox faces.
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FlameImpModel.FlameImpRenderState state) {
    }
}
