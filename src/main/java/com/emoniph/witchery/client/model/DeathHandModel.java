package com.emoniph.witchery.client.model;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import java.util.List;

public class DeathHandModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "death_hand"), "main");

    private final ModelPart rightFingerUpper;
    private final ModelPart rightFingerLower;
    private final ModelPart leftFingerUpper;
    private final ModelPart leftFingerLower;
    private final ModelPart scythe;
    private final List<ModelPart> commonParts;
    private final List<ModelPart> firstPersonParts;
    private final List<ModelPart> thirdPersonParts;

    public DeathHandModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.rightFingerUpper = root.getChild("right_finger_upper");
        this.rightFingerLower = root.getChild("right_finger_lower");
        this.leftFingerUpper = root.getChild("left_finger_upper");
        this.leftFingerLower = root.getChild("left_finger_lower");
        this.scythe = root.getChild("scythe");
        this.commonParts = List.of(
                root.getChild("wrist"),
                root.getChild("palm_upper"),
                root.getChild("palm_lower"),
                root.getChild("finger_1_upper"),
                root.getChild("finger_2_upper"),
                root.getChild("finger_3_upper"),
                root.getChild("finger_1_lower"),
                root.getChild("finger_2_lower"),
                root.getChild("finger_3_lower"));
        this.firstPersonParts = List.of(
                root.getChild("right_palm"),
                rightFingerUpper,
                rightFingerLower,
                root.getChild("right_thumb_upper"),
                root.getChild("right_thumb_lower"));
        this.thirdPersonParts = List.of(
                root.getChild("left_palm"),
                leftFingerUpper,
                leftFingerLower,
                root.getChild("left_thumb_upper"),
                root.getChild("left_thumb_lower"));
    }

    public void setup(boolean firstPerson, boolean deployed) {
        rightFingerUpper.setPos(3.0F, deployed ? 4.0F : 0.0F, deployed ? -4.0F : -5.0F);
        rightFingerLower.setPos(3.0F, deployed ? 1.0F : -1.0F, deployed ? -4.0F : -8.0F);
        leftFingerUpper.setPos(-5.0F, deployed ? 4.0F : 0.0F, deployed ? -4.0F : -5.0F);
        leftFingerLower.setPos(-5.0F, deployed ? 1.0F : -1.0F, deployed ? -4.0F : -8.0F);

        scythe.visible = deployed;
        scythe.x = firstPerson ? 6.0F : -5.0F;
        scythe.z = -3.0F;
        scythe.y = 2.0F;
        LegacyModelTransforms.setRenderWithRotation(scythe, (float) Math.PI,
                firstPerson ? (float) -Math.PI : 0.0F, (float) (-Math.PI / 2));
    }

    public List<ModelPart> handParts(boolean firstPerson) {
        java.util.ArrayList<ModelPart> parts = new java.util.ArrayList<>(
                commonParts.size() + (firstPerson ? firstPersonParts.size() : thirdPersonParts.size()));
        parts.addAll(commonParts);
        parts.addAll(firstPerson ? firstPersonParts : thirdPersonParts);
        return parts;
    }

    public ModelPart scythe() {
        return scythe;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Source uses -0.3 on every axis. Keep that cross-section, but preserve Z endpoints because the
        // modern cube baker otherwise opens visible seams between the source-authored joints.
        CubeDeformation thinConnected = new CubeDeformation(-0.3F, -0.3F, 0.0F);

        root.addOrReplaceChild("wrist", mirrored(0, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 5, 5, 2),
                PartPose.offset(-3.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("palm_upper", mirrored(0, 7)
                        .addBox(0.0F, 0.0F, 0.0F, 5, 1, 5, thinConnected),
                PartPose.offset(-3.0F, 0.0F, -5.0F));
        root.addOrReplaceChild("palm_lower", mirrored(0, 13)
                        .addBox(0.0F, 0.0F, 0.0F, 5, 2, 3, thinConnected),
                PartPose.offset(-3.0F, 1.0F, -3.0F));
        root.addOrReplaceChild("finger_1_upper", mirrored(0, 18)
                        .addBox(0.0F, 0.0F, -2.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -3.0F, 1.0F, -7.0F, 0.4833219F, 0.0F, 0.0F));
        root.addOrReplaceChild("finger_2_upper", mirrored(6, 19)
                        .addBox(0.0F, 0.0F, -2.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -1.0F, 1.0F, -7.0F, 0.4833219F, 0.0F, 0.0F));
        root.addOrReplaceChild("finger_3_upper", mirrored(12, 18)
                        .addBox(0.0F, 0.0F, -2.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        1.0F, 1.0F, -7.0F, 0.4833219F, 0.0F, 0.0F));
        root.addOrReplaceChild("finger_1_lower", mirrored(0, 23)
                        .addBox(0.0F, 0.0F, -4.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -3.0F, 2.0F, -9.0F, 2.044824F, 0.0F, 0.0F));
        root.addOrReplaceChild("finger_2_lower", mirrored(6, 24)
                        .addBox(0.0F, 0.0F, -4.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -1.0F, 2.0F, -9.0F, 2.044824F, 0.0F, 0.0F));
        root.addOrReplaceChild("finger_3_lower", mirrored(12, 23)
                        .addBox(0.0F, 0.0F, -4.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        1.0F, 2.0F, -9.0F, 2.044824F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_palm", mirrored(16, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 2, 1, 6, thinConnected),
                PartPose.offset(2.0F, 0.0F, -5.0F));
        root.addOrReplaceChild("right_finger_upper", mirrored(20, 7)
                        .addBox(0.0F, 0.0F, -4.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        3.0F, 0.0F, -5.0F, -0.5205006F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_finger_lower", mirrored(20, 12)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        3.0F, -1.0F, -8.0F, -2.732628F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_thumb_upper", mirrored(22, 17)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 3, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        3.5F, 1.0F, 0.0F, -2.7F, -0.7F, -0.669215F));
        root.addOrReplaceChild("right_thumb_lower", mirrored(22, 21)
                        .addBox(0.0F, -1.0F, -1.0F, 1, 1, 3, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        5.0F, 3.0F, -2.0F, -1.896109F, 0.2602503F, 0.3717861F));

        root.addOrReplaceChild("left_palm", mirrored(16, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 2, 1, 6, thinConnected),
                PartPose.offset(-5.0F, 0.0F, -5.0F));
        root.addOrReplaceChild("left_finger_upper", mirrored(20, 7)
                        .addBox(0.0F, 0.0F, -4.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -5.0F, 0.0F, -5.0F, -0.5205006F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_finger_lower", mirrored(20, 12)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 4, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -5.0F, -1.0F, -8.0F, -2.732628F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_thumb_upper", mirrored(22, 17)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 3, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -5.0F, 1.0F, 0.0F, -1.7F, 0.8F, 0.148711F));
        root.addOrReplaceChild("left_thumb_lower", mirrored(22, 21)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 3, thinConnected),
                LegacyModelTransforms.renderWithRotation(
                        -6.0F, 4.0F, -1.0F, -2.082002F, 0.0371828F, -0.6320403F));

        root.addOrReplaceChild("scythe", mirrored(58, 5)
                        .addBox(-0.5F, -16.0F, -0.5F, 1, 35, 1)
                        .texOffs(36, 0).addBox(0.0F, -16.0F, 0.0F, 13, 4, 0),
                PartPose.offset(-6.0F, 10.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder mirrored(int u, int v) {
        return CubeListBuilder.create().mirror().texOffs(u, v);
    }

}
