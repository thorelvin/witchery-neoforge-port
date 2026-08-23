package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Shared ghost model used by Banshee and Spectre.
 * Ported from the original ModelSpectre (64x32 texture).
 */
public class GhostModel extends EntityModel<GhostModel.GhostRenderState> {

    public static class GhostRenderState extends LivingEntityRenderState {
        public boolean screaming;
        public boolean obscured;
    }

    public static final ModelLayerLocation LAYER_BANSHEE =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "banshee"), "main");
    public static final ModelLayerLocation LAYER_SPECTRE =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "spectre"), "main");
    public static final ModelLayerLocation LAYER_POLTERGEIST =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "poltergeist"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart mouth;
    private final boolean reachingArms;

    public GhostModel(ModelPart root, boolean reachingArms) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.mouth = this.head.getChild("mouth");
        this.reachingArms = reachingArms;
    }

    public GhostModel(ModelPart root) {
        this(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("mouth",
                CubeListBuilder.create().mirror().texOffs(56, 0).addBox(0, 0, 0, 4, 5, 0),
                PartPose.offset(-2, -4, -4.02f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().mirror().texOffs(16, 0).addBox(-4, 0, -2, 8, 10, 4),
                PartPose.offset(0, 0, 0));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-3, -2, -2, 4, 12, 4),
                PartPose.offsetAndRotation(-5, 2, 0, -1.396263F, 0, 0));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1, -2, -2, 4, 12, 4),
                PartPose.offsetAndRotation(5, 2, 0, -1.396263F, 0, 0));

        root.addOrReplaceChild("robe_upper",
                CubeListBuilder.create().mirror().texOffs(38, 9).addBox(-4, 0, -2, 8, 6, 5),
                PartPose.offset(0, 10, 0));

        root.addOrReplaceChild("robe_lower",
                CubeListBuilder.create().mirror().texOffs(32, 20).addBox(-5, 0, -2, 10, 6, 6),
                PartPose.offset(0, 16, 0));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(GhostRenderState state) {
        float ageInTicks = state.ageInTicks;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        mouth.visible = state.screaming;

        float targetArmXRot = reachingArms ? -1.5F : -0.2F;
        float armBob = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot = targetArmXRot + armBob;
        leftArm.xRot = targetArmXRot - armBob;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;
        rightArm.zRot = !reachingArms && state.screaming ? 1.0F : 0.0F;
        leftArm.zRot = !reachingArms && state.screaming ? -1.0F : 0.0F;
    }
}
