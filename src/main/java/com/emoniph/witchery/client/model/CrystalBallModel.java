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

public class CrystalBallModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "crystal_ball"), "main");

    private final List<ModelPart> baseParts;
    private final ModelPart globeInner;
    private final ModelPart globeMiddle;
    private final ModelPart globeOuter;

    public CrystalBallModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        baseParts = List.of(root.getChild("base_bottom"), root.getChild("base_middle"), root.getChild("base_top"));
        globeInner = root.getChild("globe_inner");
        globeMiddle = root.getChild("globe_middle");
        globeOuter = root.getChild("globe_outer");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("base_bottom", CubeListBuilder.create().texOffs(0,25).addBox(0,0,0,6,1,6), PartPose.offset(-3f,23f,-3f));
        root.addOrReplaceChild("base_middle", CubeListBuilder.create().texOffs(0,20).addBox(0,0,0,4,1,4), PartPose.offset(-2f,22f,-2f));
        root.addOrReplaceChild("base_top",    CubeListBuilder.create().texOffs(0,17).addBox(0,0,0,2,1,2), PartPose.offset(-1f,21f,-1f));
        root.addOrReplaceChild("globe_inner", CubeListBuilder.create().texOffs(4,0) .addBox(0,0,0,2,2,2), PartPose.offset(-1f,17f,-1f));
        root.addOrReplaceChild("globe_middle",CubeListBuilder.create().texOffs(12,0).addBox(0,0,0,4,4,4), PartPose.offset(-2f,16f,-2f));
        root.addOrReplaceChild("globe_outer", CubeListBuilder.create().texOffs(8,8) .addBox(0,0,0,6,6,6), PartPose.offset(-3f,15f,-3f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public List<ModelPart> baseParts() {
        return baseParts;
    }

    public ModelPart globeInner() {
        return globeInner;
    }

    public ModelPart globeMiddle() {
        return globeMiddle;
    }

    public ModelPart globeOuter() {
        return globeOuter;
    }
}
