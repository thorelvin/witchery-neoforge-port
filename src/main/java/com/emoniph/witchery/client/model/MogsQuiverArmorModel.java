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
 * Worn Mog's Quiver model, ported from source {@code ModelGoblinClothes}.
 *
 * <p>The old chest slot rendered a lightly inflated biped torso/arms plus the quiver,
 * two arrows, and fletching parented to the body. The source texture sheet leaves most
 * of the biped surfaces transparent, so the visible silhouette is the back-strapped gear.
 */
public class MogsQuiverArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "mogs_quiver_armor"), "main");

    public MogsQuiverArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.61F), 0.0F);
        PartDefinition body = mesh.getRoot().getChild("body");

        body.addOrReplaceChild("quiver", CubeListBuilder.create()
                        .texOffs(33, 0).addBox(-2.0F, -3.0F, 0.0F, 4, 7, 1),
                PartPose.offsetAndRotation(0.0F, 7.0F, 2.0F, 0.0F, 0.0F, -0.3490659F));
        body.addOrReplaceChild("arrow1", CubeListBuilder.create()
                        .texOffs(44, 4).addBox(0.5F, -5.0F, 0.0F, 1, 2, 1),
                PartPose.offsetAndRotation(0.0F, 7.0F, 2.0F, 0.0F, 0.0F, -0.3490659F));
        body.addOrReplaceChild("arrow2", CubeListBuilder.create()
                        .texOffs(44, 4).addBox(-1.5F, -5.0F, 0.0F, 1, 2, 1),
                PartPose.offsetAndRotation(0.0F, 7.0F, 2.0F, 0.0F, 0.0F, -0.3490659F));
        body.addOrReplaceChild("feathers", CubeListBuilder.create()
                        .texOffs(44, 0).addBox(-2.0F, -7.0F, 0.0F, 4, 2, 1),
                PartPose.offsetAndRotation(0.0F, 7.0F, 2.0F, 0.0F, 0.0F, -0.3490659F));

        return LayerDefinition.create(mesh, 64, 32);
    }
}
