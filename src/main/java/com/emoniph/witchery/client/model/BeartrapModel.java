package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BeartrapModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "beartrap"), "main");

    private static final float CLOSED_JAW_ROTATION = 1.2F;
    private static final float OPEN_PLATE_Y = 23.2F;
    private static final float CLOSED_PLATE_Y = 23.8F;

    private final ModelPart plate;
    private final ModelPart armFront;
    private final ModelPart armBack;

    public BeartrapModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.plate = root.getChild("plate");
        this.armFront = root.getChild("arm_front");
        this.armBack = root.getChild("arm_back");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(1, 0)
                        .addBox(-2.0F, -1.5F, -2.0F, 4, 1, 4),
                LegacyModelTransforms.render(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-5.0F, -1.0F, -0.5F, 10, 1, 1),
                LegacyModelTransforms.render(0.0F, 23.99F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("disk_left", CubeListBuilder.create().texOffs(19, 3)
                        .addBox(3.7F, -2.0F, -1.0F, 1, 2, 2),
                LegacyModelTransforms.render(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("disk_right", CubeListBuilder.create().texOffs(19, 3)
                        .addBox(-4.7F, -2.0F, -1.0F, 1, 2, 2),
                LegacyModelTransforms.render(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition armFront = root.addOrReplaceChild("arm_front", CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-4.5F, -1.0F, -7.0F, 9, 1, 1),
                LegacyModelTransforms.render(0.0F, 23.99F, 0.0F, 0.0F, 0.0F, 0.0F));
        addJawChildren(armFront, "front", -7.0F, -6.0F);

        PartDefinition armBack = root.addOrReplaceChild("arm_back", CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-4.5F, -1.0F, 6.0F, 9, 1, 1),
                LegacyModelTransforms.render(0.0F, 23.99F, 0.0F, 0.0F, 0.0F, 0.0F));
        addJawChildren(armBack, "back", 6.0F, 0.0F);

        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addJawChildren(PartDefinition arm, String suffix, float toothZ, float railZ) {
        arm.addOrReplaceChild("arm_right_" + suffix, CubeListBuilder.create().texOffs(0, 12)
                        .addBox(-4.5F, -1.0F, railZ, 1, 1, 6),
                PartPose.ZERO);
        arm.addOrReplaceChild("arm_left_" + suffix, CubeListBuilder.create().texOffs(0, 12)
                        .addBox(3.5F, -1.0F, railZ, 1, 1, 6),
                PartPose.ZERO);

        float[] toothX = {-4.5F, -2.5F, -0.5F, 1.5F, 3.5F};
        for (int i = 0; i < toothX.length; i++) {
            arm.addOrReplaceChild("arm_tooth_" + (i + 1) + "_" + suffix,
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(toothX[i], -2.0F, toothZ, 1, 1, 1),
                    PartPose.ZERO);
        }
    }

    public void setClosed(boolean closed) {
        plate.y = closed ? CLOSED_PLATE_Y : OPEN_PLATE_Y;
        armFront.xRot = closed ? -CLOSED_JAW_ROTATION : 0.0F;
        armBack.xRot = closed ? CLOSED_JAW_ROTATION : 0.0F;
    }
}
