package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
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
    private final ModelPart sac1;
    private final ModelPart sac2;
    private final ModelPart sac3;

    public LeechChestModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        lidBl = root.getChild("chest_lid_bl");
        lidFr = root.getChild("chest_lid_fr");
        lidBr = root.getChild("chest_lid_br");
        lidFl = root.getChild("chest_lid_fl");
        sac1 = root.getChild("sac1");
        sac2 = root.getChild("sac2");
        sac3 = root.getChild("sac3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("chest_below", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0, 0, 0, 14, 9, 14),
                LegacyModelTransforms.render(1, 7, 1, 0, 0, 0));
        root.addOrReplaceChild("chest_lid_bl", CubeListBuilder.create().texOffs(28, 24)
                        .addBox(-6, -5, -6, 6, 5, 6),
                LegacyModelTransforms.render(14, 7, 14, 0, 0, 0));
        root.addOrReplaceChild("chest_lid_fr", CubeListBuilder.create().texOffs(0, 36)
                        .addBox(0, -5, 0, 6, 5, 6),
                LegacyModelTransforms.render(2, 7, 2, 0, 0, 0));
        root.addOrReplaceChild("chest_lid_br", CubeListBuilder.create().texOffs(0, 24)
                        .addBox(0, -5, -6, 6, 5, 6),
                LegacyModelTransforms.render(2, 7, 14, 0, 0, 0));
        root.addOrReplaceChild("chest_lid_fl", CubeListBuilder.create().texOffs(28, 36)
                        .addBox(-6, -5, 0, 6, 5, 6),
                LegacyModelTransforms.render(14, 7, 2, 0, 0, 0));
        root.addOrReplaceChild("sac1", CubeListBuilder.create().texOffs(0, 8)
                        .addBox(0, 0, 0, 2, 3, 1),
                LegacyModelTransforms.render(3, 8, 0, 0, 0, 0));
        root.addOrReplaceChild("sac2", CubeListBuilder.create().texOffs(0, 3)
                        .addBox(0, 0, 0, 3, 2, 1),
                LegacyModelTransforms.render(9, 13, 0, 0, 0, 0));
        root.addOrReplaceChild("sac3", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0, 0, 0, 2, 1, 1),
                LegacyModelTransforms.render(9, 9, 0, 0, 0, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setLidAngle(float openness) {
        float inverse = 1.0F - openness;
        float eased = 1.0F - inverse * inverse * inverse;
        float full = eased * Mth.HALF_PI;
        float wide = full / 1.5F;
        float narrow = full / 3.0F;

        lidBl.setRotation(-wide, narrow, wide);
        lidBr.setRotation(-wide, -narrow, -wide);
        lidFl.setRotation(wide, -narrow, wide);
        lidFr.setRotation(wide, narrow, -wide);
    }

    public void setSacCount(int count) {
        sac1.visible = count >= 1;
        sac2.visible = count >= 2;
        sac3.visible = count >= 3;
    }
}
