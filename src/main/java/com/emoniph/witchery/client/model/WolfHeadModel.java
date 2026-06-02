package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Mounted wolf/hellhound head model.
 *
 * Source parity: {@code ModelWolfHead} (1.7.10-0.24.1) — itself the vanilla wolf head submodel
 * (head 6x6x4 @ texOffs 0,0; two ears 2x2x1 @ 16,14; snout 3x3x4 @ 0,10) on a 64x32 texture,
 * so the wolf variant reuses the vanilla wolf texture and the hellhound variant uses
 * {@code witchery:textures/entity/hellhound.png}.
 */
public class WolfHeadModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "wolf_head"), "main");

    private final ModelPart head;

    public WolfHeadModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -6.0F, 0.0F, 6.0F, 6.0F, 4.0F)
                        .texOffs(16, 14).addBox(-3.0F, -8.0F, 3.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(16, 14).addBox(1.0F, -8.0F, 3.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(0, 10).addBox(-1.5F, -3.1F, -3.0F, 3.0F, 3.0F, 4.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public void setYaw(float degrees) {
        this.head.yRot = degrees * ((float) Math.PI / 180.0F);
        this.head.xRot = 0.0F;
    }
}
