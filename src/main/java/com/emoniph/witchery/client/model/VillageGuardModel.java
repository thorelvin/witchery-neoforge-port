package com.emoniph.witchery.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Exact shared geometry for Witchery's 1.7.10 village guard and vampire models. */
public class VillageGuardModel<S extends LegacyBipedModel.State> extends LegacyBipedModel<S> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "village_guard"), "main");

    private final ModelPart robe;
    private final ModelPart sourceHat;
    private final boolean guardVariant;

    public VillageGuardModel(ModelPart root, boolean guardVariant) {
        super(root);
        this.robe = root.getChild("robe");
        this.sourceHat = root.getChild("source_hat");
        this.guardVariant = guardVariant;
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = LegacyBipedModel.createLegacyMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(28, 46)
                        .addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8),
                PartPose.ZERO);
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(52, 46)
                        .addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("source_hat",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        root.addOrReplaceChild("robe",
                CubeListBuilder.create().texOffs(0, 38)
                        .addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, new CubeDeformation(0.5F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        hat.visible = false;
        sourceHat.setPos(head.x, head.y + 0.3F, head.z);
        sourceHat.setRotation(head.xRot, head.yRot, head.zRot);
        sourceHat.visible = true;
        robe.setPos(0.0F, 0.0F, 0.0F);
        robe.setRotation(0.0F, 0.0F, 0.0F);
        robe.visible = guardVariant;
        if (guardVariant) {
            rightLeg.xRot *= 0.5F;
            leftLeg.xRot *= 0.5F;
            rightLeg.yRot = 0.0F;
            leftLeg.yRot = 0.0F;
        }
    }
}
