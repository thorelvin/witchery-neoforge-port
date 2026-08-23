package com.emoniph.witchery.client.model;

import com.emoniph.witchery.blocks.DreamWeaveType;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

public class DreamCatcherModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "dream_catcher"), "main");

    private final List<ModelPart> frameParts;
    private final List<ModelPart> nets;
    private final ModelPart decoration;

    public DreamCatcherModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        frameParts = List.of(
            root.getChild("frame_left"), root.getChild("frame_right"),
            root.getChild("frame_top"), root.getChild("frame_bottom")
        );
        nets = List.of(
            root.getChild("net_fleet_foot"), root.getChild("net_iron_arm"),
            root.getChild("net_fasting"), root.getChild("net_nightmares"),
            root.getChild("net_intensity")
        );
        decoration = root.getChild("decoration");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("frame_left",   CubeListBuilder.create().texOffs(0,2).addBox(0,0,0,1,8,1), PartPose.offset(-4f,10f,7f));
        root.addOrReplaceChild("frame_right",  CubeListBuilder.create().texOffs(0,2).addBox(0,0,0,1,8,1), PartPose.offset(3f,10f,7f));
        root.addOrReplaceChild("frame_top",    CubeListBuilder.create().texOffs(0,0).addBox(0,0,0,6,1,1), PartPose.offset(-3f,10f,7f));
        root.addOrReplaceChild("frame_bottom", CubeListBuilder.create().texOffs(0,0).addBox(0,0,0,6,1,1), PartPose.offset(-3f,17f,7f));
        root.addOrReplaceChild("net_fleet_foot", CubeListBuilder.create().texOffs(17,10).addBox(0,0,0,6,6,0), PartPose.offset(-3f,11f,8f));
        root.addOrReplaceChild("net_iron_arm",   CubeListBuilder.create().texOffs(17,4).addBox(0,0,0,6,6,0), PartPose.offset(-3f,11f,8f));
        root.addOrReplaceChild("net_fasting",    CubeListBuilder.create().texOffs(17,16).addBox(0,0,0,6,6,0), PartPose.offset(-3f,11f,8f));
        root.addOrReplaceChild("net_nightmares", CubeListBuilder.create().texOffs(4,4).addBox(0,0,0,6,6,0), PartPose.offset(-3f,11f,8f));
        root.addOrReplaceChild("net_intensity",  CubeListBuilder.create().texOffs(17,22).addBox(0,0,0,6,6,0), PartPose.offset(-3f,11f,8f));
        // Decoration is a D=0 flat face, matching the original zero-depth model.
        root.addOrReplaceChild("decoration",   CubeListBuilder.create().texOffs(0,12).addBox(0,0,0,8,6,0), PartPose.offset(-4f,18f,7f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public List<ModelPart> frameParts() {
        return frameParts;
    }

    public ModelPart net(DreamWeaveType weave) {
        return nets.get(weave.id);
    }

    public ModelPart decoration() {
        return decoration;
    }
}
