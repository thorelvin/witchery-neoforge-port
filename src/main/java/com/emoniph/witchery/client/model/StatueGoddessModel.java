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

public class StatueGoddessModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "statue_goddess"), "main");

    private final List<ModelPart> parts;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public StatueGoddessModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        parts = List.of(
            root.getChild("head"),
            root.getChild("cleave"),
            root.getChild("body"),
            root.getChild("right_arm"),
            root.getChild("left_arm"),
            root.getChild("right_leg"),
            root.getChild("left_leg"),
            root.getChild("dress_lower"),
            root.getChild("dress_middle")
        );
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -8f, -4f, 8, 8, 8),
                PartPose.offset(0f, 0f, 0f));

        // Decorative ornament, rotated -45° around X
        root.addOrReplaceChild("cleave",
                CubeListBuilder.create().texOffs(35, 0).addBox(0f, -2f, -2f, 8, 4, 4),
                PartPose.offsetAndRotation(-4f, 3f, -2f, -(float) Math.PI / 4, 0f, 0f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4f, 0f, -2f, 8, 12, 4),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3f, -2f, -2f, 4, 12, 4),
                PartPose.offset(-5f, 2f, 0f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1f, -2f, -2f, 4, 12, 4),
                PartPose.offset(5f, 2f, 0f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2f, 0f, -2f, 4, 12, 4),
                PartPose.offset(-2f, 12f, 0f));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2f, 0f, -2f, 4, 12, 4),
                PartPose.offset(2f, 12f, 0f));

        root.addOrReplaceChild("dress_lower",
                CubeListBuilder.create().texOffs(0, 45).addBox(-6f, 0f, -4f, 12, 7, 8),
                PartPose.offset(0f, 17f, 0f));

        root.addOrReplaceChild("dress_middle",
                CubeListBuilder.create().texOffs(0, 33).addBox(-5f, 0f, -3f, 10, 5, 6),
                PartPose.offset(0f, 12f, 0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setupSourcePose() {
        leftArm.xRot = -0.6F;
        leftArm.yRot = -0.6F;
        leftArm.zRot = -0.1F;
        rightArm.xRot = -0.6F;
        rightArm.yRot = 0.6F;
        rightArm.zRot = 0.1F;
    }
}
