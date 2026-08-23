package com.emoniph.witchery.client.model;

import com.emoniph.witchery.WitcheryMod;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** The 20 source bark pieces rendered by a charged Bark Belt. */
public final class BarkGrowthModel {
    public static final ModelLayerLocation TALL_UV_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "bark_growth_tall_uv"), "main");
    public static final ModelLayerLocation WIDE_UV_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "bark_growth_wide_uv"), "main");

    public enum Group {
        BODY,
        SPIKES,
        RIGHT_LEG,
        LEFT_LEG,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_HEAD_SHOULDER,
        LEFT_HEAD_SHOULDER
    }

    private final List<ModelPart> body;
    private final List<ModelPart> spikes;
    private final List<ModelPart> rightLeg;
    private final List<ModelPart> leftLeg;
    private final List<ModelPart> rightArm;
    private final List<ModelPart> leftArm;
    private final List<ModelPart> rightHeadShoulder;
    private final List<ModelPart> leftHeadShoulder;

    public BarkGrowthModel(ModelPart tallUvRoot, ModelPart wideUvRoot) {
        this.body = List.of(tallUvRoot.getChild("body_front"), tallUvRoot.getChild("body_back"));
        this.spikes = List.of(
                wideUvRoot.getChild("spike_lower_left"),
                wideUvRoot.getChild("spike_lower_right"),
                wideUvRoot.getChild("spike_upper_left"),
                wideUvRoot.getChild("spike_upper_right"));
        this.rightLeg = List.of(
                tallUvRoot.getChild("leg_right_front"), tallUvRoot.getChild("leg_right_back"));
        this.leftLeg = List.of(
                tallUvRoot.getChild("leg_left_front"), tallUvRoot.getChild("leg_left_back"));
        this.rightArm = List.of(
                tallUvRoot.getChild("arm_right_front"),
                wideUvRoot.getChild("arm_right_out"),
                tallUvRoot.getChild("arm_right_back"));
        this.leftArm = List.of(
                tallUvRoot.getChild("arm_left_back"),
                tallUvRoot.getChild("arm_left_front"),
                wideUvRoot.getChild("arm_left_out"));
        this.rightHeadShoulder = List.of(
                tallUvRoot.getChild("head_right"), wideUvRoot.getChild("shoulder_right"));
        this.leftHeadShoulder = List.of(
                tallUvRoot.getChild("head_left"), wideUvRoot.getChild("shoulder_left"));
    }

    public List<ModelPart> parts(Group group) {
        return switch (group) {
            case BODY -> body;
            case SPIKES -> spikes;
            case RIGHT_LEG -> rightLeg;
            case LEFT_LEG -> leftLeg;
            case RIGHT_ARM -> rightArm;
            case LEFT_ARM -> leftArm;
            case RIGHT_HEAD_SHOULDER -> rightHeadShoulder;
            case LEFT_HEAD_SHOULDER -> leftHeadShoulder;
        };
    }

    /** Source pieces that call setTextureSize(64, 128). */
    public static LayerDefinition createTallUvLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        add(root, "head_right", 124, 0, -0.5F, -5.0F, -0.5F, 1, 5, 1,
                -4.0F, 0.0F, 0.0F, -0.1487144F, 0.0F, -0.4089647F);
        add(root, "head_left", 124, 0, -0.5F, -5.0F, -0.5F, 1, 5, 1,
                4.0F, 0.0F, 0.0F, -0.1487144F, 0.0F, 0.4089647F);
        add(root, "leg_right_front", 95, 0, 0.0F, 0.0F, 0.0F, 2, 7, 1,
                -4.0F, 13.0F, -3.0F, 0.0F, 0.0F, -0.2230717F);
        add(root, "leg_left_front", 95, 0, 0.0F, 0.0F, 0.0F, 2, 7, 1,
                1.0F, 13.0F, -3.0F, 0.0F, 0.0F, 0.1230717F);
        add(root, "leg_right_back", 95, 0, 0.0F, 0.0F, 0.0F, 2, 7, 1,
                -4.0F, 13.0F, 2.0F, 0.0F, 0.0F, -0.2230717F);
        add(root, "leg_left_back", 95, 0, 0.0F, 0.0F, 0.0F, 2, 7, 1,
                1.0F, 13.0F, 2.0F, 0.0F, 0.0F, 0.1230717F);
        add(root, "body_front", 111, 0, 0.0F, 0.0F, 0.0F, 6, 9, 1,
                -2.5F, 1.0F, -3.1F, 0.0F, 0.0F, 0.1487144F);
        add(root, "body_back", 111, 0, 0.0F, 0.0F, 0.0F, 6, 9, 1,
                -2.5F, 1.0F, 2.1F, 0.0F, 0.0F, 0.0887144F);
        add(root, "arm_right_front", 102, 0, 0.0F, 0.0F, 0.0F, 3, 7, 1,
                -8.0F, 3.0F, -3.0F, 0.0F, 0.0F, -0.1487144F);
        add(root, "arm_left_front", 102, 0, 0.0F, 0.0F, 0.0F, 3, 6, 1,
                5.0F, 2.0F, -3.0F, 0.0F, 0.0F, 0.0687144F);
        add(root, "arm_right_back", 102, 0, 0.0F, 0.0F, 0.0F, 3, 7, 1,
                -8.0F, 3.0F, 2.0F, 0.0F, 0.0F, -0.1487144F);
        add(root, "arm_left_back", 102, 0, 0.0F, 0.0F, 0.0F, 3, 6, 1,
                5.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0687144F);
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Source pieces that call setTextureSize(128, 64). */
    public static LayerDefinition createWideUvLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        add(root, "arm_left_out", 120, 0, 0.0F, 0.0F, 0.0F, 1, 7, 3,
                8.0F, 2.0F, -1.5F, 0.0371786F, 0.0F, 0.0F);
        add(root, "arm_right_out", 120, 0, 0.0F, 0.0F, 0.0F, 1, 6, 3,
                -9.0F, 2.0F, -1.0F, -0.1858931F, 0.0F, 0.0F);
        add(root, "spike_lower_right", 120, 0, -0.5F, -6.0F, -0.5F, 1, 6, 1,
                -1.0F, 7.0F, 2.0F, -0.7807508F, -0.1858931F, 0.0F);
        add(root, "spike_lower_left", 120, 0, -0.5F, -6.0F, -0.5F, 1, 6, 1,
                1.0F, 7.0F, 2.0F, -0.7807508F, 0.1858931F, 0.0F);
        add(root, "spike_upper_left", 120, 0, -0.5F, -6.0F, -0.5F, 1, 6, 1,
                2.0F, 3.0F, 2.0F, -0.7807508F, 0.1858931F, 0.0F);
        add(root, "spike_upper_right", 120, 0, -0.5F, -6.0F, -0.5F, 1, 6, 1,
                -2.0F, 3.0F, 2.0F, -0.7807508F, -0.1858931F, 0.0F);
        add(root, "shoulder_right", 108, 0, 0.0F, 0.0F, 0.0F, 5, 1, 5,
                -9.0F, -1.5F, -2.5F, 0.0371786F, -0.1115358F, -0.1230717F);
        add(root, "shoulder_left", 108, 0, 0.0F, 0.0F, 0.0F, 5, 1, 5,
                4.0F, -2.5F, -1.5F, 0.0F, 0.2974289F, 0.1830717F);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void add(PartDefinition root, String name, int u, int v,
                            float boxX, float boxY, float boxZ,
                            int width, int height, int depth,
                            float pivotX, float pivotY, float pivotZ,
                            float xRot, float yRot, float zRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                        .addBox(boxX, boxY, boxZ, width, height, depth),
                PartPose.offsetAndRotation(pivotX, pivotY, pivotZ, xRot, yRot, zRot));
    }
}
