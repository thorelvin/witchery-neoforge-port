package com.emoniph.witchery.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Single-block coffin model. UV layout matches original Witchery 1.7.10 ModelCoffin.
 * Texture sheet: 128×64.
 * Rendered entirely from the HEAD block entity; FOOT block renders nothing.
 */
public class CoffinModel extends Model.Simple {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "coffin"), "main");

    private final ModelPart base;
    private final ModelPart baseLower;
    private final ModelPart lid;
    private final ModelPart sideEnd;
    private final ModelPart sideLeft;
    private final ModelPart sideRight;

    public CoffinModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.base      = root.getChild("base");
        this.baseLower = root.getChild("base_lower");
        this.lid       = root.getChild("lid");
        this.sideEnd   = root.getChild("side_end");
        this.sideLeft  = root.getChild("side_left");
        this.sideRight = root.getChild("side_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Main interior box
        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7f, 0f, -8f, 14, 5, 15),
                PartPose.offset(0f, 3f, 0f));

        // Bottom floor plate
        root.addOrReplaceChild("base_lower",
                CubeListBuilder.create().texOffs(0, 20).addBox(-8f, 0f, -8f, 16, 1, 16),
                PartPose.offset(0f, 8f, 0f));

        // Lid — animates open around Z axis; children ride with it
        PartDefinition lid = root.addOrReplaceChild("lid",
                CubeListBuilder.create().texOffs(60, 0).addBox(-1f, 0f, -8f, 16, 1, 16),
                PartPose.offset(-7f, -5f, 0f));

        lid.addOrReplaceChild("lid_mid",
                CubeListBuilder.create().texOffs(64, 18).addBox(0f, -1f, -8f, 14, 1, 15),
                PartPose.ZERO);

        lid.addOrReplaceChild("lid_top",
                CubeListBuilder.create().texOffs(67, 35).addBox(1f, -2f, -8f, 12, 1, 14),
                PartPose.ZERO);

        // Head-end wall
        root.addOrReplaceChild("side_end",
                CubeListBuilder.create().texOffs(33, 51).addBox(-6f, 0f, 6f, 12, 7, 1),
                PartPose.offset(0f, -4f, 0f));

        // Right side wall
        root.addOrReplaceChild("side_right",
                CubeListBuilder.create().texOffs(0, 37).addBox(-7f, 0f, -8f, 1, 7, 15),
                PartPose.offset(0f, -4f, 0f));

        // Left side wall (mirrored UV)
        root.addOrReplaceChild("side_left",
                CubeListBuilder.create().texOffs(0, 37).mirror().addBox(6f, 0f, -8f, 1, 7, 15),
                PartPose.offset(0f, -4f, 0f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    /** @param openAmount 0=closed, 1=fully open (lid rotates -90° around Z axis). */
    public void setOpen(float openAmount) {
        lid.zRot = -(openAmount * Mth.HALF_PI);
    }

    }
