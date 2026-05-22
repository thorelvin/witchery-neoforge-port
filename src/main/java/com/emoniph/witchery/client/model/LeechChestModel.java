package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class LeechChestModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "leech_chest"), "main");

    private final ModelPart lidBl;
    private final ModelPart lidFr;
    private final ModelPart lidBr;
    private final ModelPart lidFl;

    public LeechChestModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        lidBl = root.getChild("chest_lid_bl");
        lidFr = root.getChild("chest_lid_fr");
        lidBr = root.getChild("chest_lid_br");
        lidFl = root.getChild("chest_lid_fl");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("chest_below",  CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,14,9,14),    PartPose.offset(1f,7f,1f));
        root.addOrReplaceChild("chest_lid_bl", CubeListBuilder.create().texOffs(28,24).addBox(-6f,-5f,-6f,6,5,6), PartPose.offset(14f,7f,14f));
        root.addOrReplaceChild("chest_lid_fr", CubeListBuilder.create().texOffs(0,36) .addBox(0,-5f,0,6,5,6),     PartPose.offset(2f,7f,2f));
        root.addOrReplaceChild("chest_lid_br", CubeListBuilder.create().texOffs(0,24) .addBox(0,-5f,-6f,6,5,6),   PartPose.offset(2f,7f,14f));
        root.addOrReplaceChild("chest_lid_fl", CubeListBuilder.create().texOffs(28,36).addBox(-6f,-5f,0,6,5,6),   PartPose.offset(14f,7f,2f));
        root.addOrReplaceChild("sac1",         CubeListBuilder.create().texOffs(0,8)  .addBox(0,0,0,2,3,1),       PartPose.offset(3f,8f,0f));
        root.addOrReplaceChild("sac2",         CubeListBuilder.create().texOffs(0,3)  .addBox(0,0,0,3,2,1),       PartPose.offset(9f,13f,0f));
        root.addOrReplaceChild("sac3",         CubeListBuilder.create().texOffs(0,0)  .addBox(0,0,0,2,1,1),       PartPose.offset(9f,9f,0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Rotate all four lid quadrants outward; angle 0=closed, 1=fully open. */
    public void setLidAngle(float angle) {
        float rot = angle * Mth.HALF_PI;
        lidBl.yRot = rot;
        lidFr.yRot = rot;
        lidBr.yRot = rot;
        lidFl.yRot = rot;
    }
}
