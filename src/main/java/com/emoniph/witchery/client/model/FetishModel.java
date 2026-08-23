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
import java.util.List;

public class FetishModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "fetish"), "main");

    private final List<ModelPart> untintedParts;
    private final List<ModelPart> tintedParts;

    public FetishModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        untintedParts = List.of(
            root.getChild("pole_vertical"),
            root.getChild("pole_horizontal"),
            root.getChild("head_inner"),
            root.getChild("arm_left_inner"),
            root.getChild("arm_right_inner")
        );
        tintedParts = List.of(
            root.getChild("head"), root.getChild("body"),
            root.getChild("arm_left"), root.getChild("arm_right")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("pole_vertical",
                CubeListBuilder.create().texOffs(0, 2).addBox(-1f, 0f, -1f, 2, 15, 2),
                PartPose.offset(0f, 9f, 0f));

        root.addOrReplaceChild("pole_horizontal",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8f, 0f, -0.5f, 16, 1, 1),
                PartPose.offset(0f, 13f, 0f));

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(12, 21).addBox(-2f, -4f, -2f, 4, 5, 4),
                PartPose.offset(0f, 12f, 0f));

        // D=0 flat inner face on head front
        root.addOrReplaceChild("head_inner",
                CubeListBuilder.create().texOffs(29, 25).addBox(-2f, -4f, -1.9f, 4, 5, 0),
                PartPose.offset(0f, 12f, 0f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(8, 2).addBox(-3f, 0f, -1.5f, 6, 9, 3),
                PartPose.offset(0f, 12.5f, 0f));

        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(0, 23).addBox(0f, -0.5f, -1.5f, 3, 4, 3),
                PartPose.offset(3f, 13f, 0f));

        // W=0 flat inner face on left arm
        root.addOrReplaceChild("arm_left_inner",
                CubeListBuilder.create().texOffs(29, 25).addBox(2.9f, -0.5f, -1.5f, 0, 4, 3),
                PartPose.offset(3f, 13f, 0f));

        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(0, 23).addBox(-3f, -0.5f, -1.5f, 3, 4, 3),
                PartPose.offset(-3f, 13f, 0f));

        // W=0 flat inner face on right arm
        root.addOrReplaceChild("arm_right_inner",
                CubeListBuilder.create().texOffs(29, 25).addBox(-2.9f, -0.5f, -1.5f, 0, 4, 3),
                PartPose.offset(-3f, 13f, 0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public List<ModelPart> untintedParts() {
        return untintedParts;
    }

    public List<ModelPart> tintedParts() {
        return tintedParts;
    }
}
