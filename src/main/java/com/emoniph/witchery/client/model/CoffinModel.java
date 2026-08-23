package com.emoniph.witchery.client.model;

import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BedPart;

public class CoffinModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "coffin"), "main");

    private final ModelPart sideLeft;
    private final ModelPart sideRight;
    private final ModelPart sideEnd;
    private final ModelPart base;
    private final ModelPart lid;
    private final ModelPart lidMid;
    private final ModelPart lidTop;
    private final List<ModelPart> renderParts;

    public CoffinModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        sideLeft = root.getChild("side_left");
        sideRight = root.getChild("side_right");
        sideEnd = root.getChild("side_end");
        base = root.getChild("base");
        lid = root.getChild("lid");
        lidTop = lid.getChild("lid_top");
        lidMid = lid.getChild("lid_mid");
        renderParts = List.of(
                sideLeft, root.getChild("base_lower"), sideEnd, base, sideRight, lid);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition lid = root.addOrReplaceChild("lid",
                CubeListBuilder.create().texOffs(60, 0).addBox(-1, 0, -8, 16, 1, 16),
                PartPose.offset(-7, -5, 0));
        lid.addOrReplaceChild("lid_top",
                CubeListBuilder.create().texOffs(67, 35).addBox(1, -2, -8, 12, 1, 14),
                PartPose.ZERO);
        lid.addOrReplaceChild("lid_mid",
                CubeListBuilder.create().texOffs(64, 18).addBox(0, -1, -8, 14, 1, 15),
                PartPose.ZERO);
        root.addOrReplaceChild("side_end",
                CubeListBuilder.create().texOffs(33, 51).addBox(-6, 0, 6, 12, 7, 1),
                PartPose.offset(0, -4, 0));
        root.addOrReplaceChild("side_right",
                CubeListBuilder.create().texOffs(0, 37).addBox(-7, 0, -8, 1, 7, 15),
                PartPose.offset(0, -4, 0));
        root.addOrReplaceChild("side_left",
                CubeListBuilder.create().texOffs(0, 37).mirror().addBox(6, 0, -8, 1, 7, 15),
                PartPose.offset(0, -4, 0));
        root.addOrReplaceChild("base_lower",
                CubeListBuilder.create().texOffs(0, 20).addBox(-8, 0, -8, 16, 1, 16),
                PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7, 0, -8, 14, 5, 15),
                PartPose.offset(0, 3, 0));

        return LayerDefinition.create(mesh, 128, 64);
    }

    public void setupSourcePose(BedPart part, float openAmount) {
        boolean head = part == BedPart.HEAD;
        sideLeft.z = head ? 0.0F : 1.0F;
        sideRight.z = head ? 0.0F : 1.0F;
        sideEnd.z = 0.0F;
        sideEnd.yRot = head ? 0.0F : Mth.PI;
        base.z = head ? 0.0F : 1.0F;
        lidTop.z = head ? 0.0F : 2.0F;
        lidMid.z = head ? 0.0F : 1.0F;
        lid.yRot = 0.0F;
        lid.zRot = sourceLidRotation(openAmount);
    }

    public static float sourceLidRotation(float openAmount) {
        float eased = 1.0F - (1.0F - openAmount) * (1.0F - openAmount) * (1.0F - openAmount);
        return -eased * Mth.HALF_PI;
    }

    public List<ModelPart> renderParts() {
        return renderParts;
    }
}
