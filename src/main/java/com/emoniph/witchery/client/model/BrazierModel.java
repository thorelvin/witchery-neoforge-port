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

public class BrazierModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "brazier"), "main");

    private final List<ModelPart> structureParts;
    private final ModelPart ash;

    public BrazierModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.ash = root.getChild("ash");
        structureParts = List.of(
            root.getChild("leg1"), root.getChild("leg2"), root.getChild("leg3"), root.getChild("leg4"),
            root.getChild("foot1"), root.getChild("foot2"), root.getChild("foot3"), root.getChild("foot4"),
            root.getChild("foot_base"), root.getChild("pan_base"),
            root.getChild("pan_side1"), root.getChild("pan_side2"),
            root.getChild("pan_side3"), root.getChild("pan_side4")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 4 vertical legs
        root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(0,0).addBox(-0.5f,0,-0.5f,1,11,1), PartPose.offset(0.7f,10f,-0.74f));
        root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(0,0).addBox(-0.5f,0,-0.5f,1,11,1), PartPose.offset(-0.7f,10f,-0.7f));
        root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(0,0).addBox(-0.5f,0,-0.5f,1,11,1), PartPose.offset(-0.7f,10f,0.7f));
        root.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(0,0).addBox(-0.5f,0,-0.5f,1,11,1), PartPose.offset(0.7f,10f,0.7f));

        // Angled feet (rotated 45° on X and Z)
        float d45 = (float)(Math.PI / 4);
        root.addOrReplaceChild("foot1", CubeListBuilder.create().texOffs(0,13).addBox(-0.5f,0,-0.5f,1,5,1), PartPose.offsetAndRotation(0.7f,21f,-0.7f, -d45,0,-d45));
        root.addOrReplaceChild("foot2", CubeListBuilder.create().texOffs(0,13).addBox(-0.5f,0,-0.5f,1,5,1), PartPose.offsetAndRotation(-0.7f,21f,-0.7f, -d45,0,d45));
        root.addOrReplaceChild("foot3", CubeListBuilder.create().texOffs(0,13).addBox(-0.5f,0,-0.5f,1,5,1), PartPose.offsetAndRotation(-0.7f,21f,0.7f, d45,0,d45));
        root.addOrReplaceChild("foot4", CubeListBuilder.create().texOffs(0,13).addBox(-0.5f,0,-0.5f,1,5,1), PartPose.offsetAndRotation(0.7f,21f,0.7f, d45,0,-d45));

        // Center foot ring
        root.addOrReplaceChild("foot_base", CubeListBuilder.create().texOffs(6,0).addBox(-1.5f,-0.5f,-1.5f,3,1,3), PartPose.offset(0,21f,0));

        // Pan
        // Modern ModelPart baking drops zero-height planes, so keep the source ash surface visibly thick.
        root.addOrReplaceChild("ash",       CubeListBuilder.create().texOffs(0,10).addBox(-2.75f,-0.08f,-2.75f,5.5f,0.16f,5.5f), PartPose.offset(0,9.35f,0));
        root.addOrReplaceChild("pan_base",  CubeListBuilder.create().texOffs(6,5) .addBox(-3f,0,-3f,6,1,6), PartPose.offset(0,9.95f,0));
        root.addOrReplaceChild("pan_side1", CubeListBuilder.create().texOffs(5,12).addBox(-0.5f,-0.5f,-3f,1,1,6), PartPose.offset(3f,9.5f,0));
        root.addOrReplaceChild("pan_side2", CubeListBuilder.create().texOffs(4,26).addBox(-3f,-0.5f,-0.5f,6,1,1), PartPose.offset(0,9.5f,3f));
        root.addOrReplaceChild("pan_side3", CubeListBuilder.create().texOffs(5,12).addBox(-0.5f,-0.5f,-3f,1,1,6), PartPose.offset(-3f,9.5f,0));
        root.addOrReplaceChild("pan_side4", CubeListBuilder.create().texOffs(4,26).addBox(-3f,-0.5f,-0.5f,6,1,1), PartPose.offset(0,9.5f,-3f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public List<ModelPart> structureParts() {
        return structureParts;
    }

    public ModelPart ashPart() {
        return ash;
    }

    public void setAshIngredientCount(int ingredientCount) {
        ash.y = 9.35f - Math.max(0, ingredientCount - 1) * 0.1f;
    }
}
