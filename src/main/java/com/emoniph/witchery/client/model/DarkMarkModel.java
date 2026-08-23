package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Dark Mark model — ported from ModelDarkMark (1.7.10-0.24.1). A single skull: a 16×12×16 cranium
 * and a 10×4×12 jaw, on a 64×64 sheet. The source renders it at 2× scale (applied in the renderer).
 */
public class DarkMarkModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "dark_mark"), "main");

    private final ModelPart skull;

    public DarkMarkModel(ModelPart root) {
        super(root);
        this.skull = root.getChild("skull");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("skull",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-8.0F, -12.0F, -8.0F, 16, 12, 16)
                        .texOffs(0, 29).addBox(-5.0F, 0.0F, -8.0F, 10, 4, 12),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        skull.yRot = state.yRot * Mth.DEG_TO_RAD;
        skull.xRot = state.xRot * Mth.DEG_TO_RAD;
    }
}
