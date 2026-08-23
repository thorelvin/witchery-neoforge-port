package com.emoniph.witchery.client.model;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
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

/** Exact source {@code ModelToad}. */
public class ToadModel extends EntityModel<ToadModel.ToadRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "toad"), "main");
    public static final ModelLayerLocation BABY_LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "toad"), "baby");

    private final ModelPart head;
    private final ModelPart armRight;
    private final ModelPart armLeft;
    private final ModelPart legRight;
    private final ModelPart legLeft;

    public ToadModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.armRight = root.getChild("arm_right");
        this.armLeft = root.getChild("arm_left");
        this.legRight = root.getChild("leg_right");
        this.legLeft = root.getChild("leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 5).addBox(-2.0F, -1.0F, -4.0F, 4, 2, 4)
                        .texOffs(0, 0).addBox(-2.5F, -3.0F, -3.0F, 2, 2, 2)
                        .texOffs(8, 0).addBox(0.5F, -3.0F, -3.0F, 2, 2, 2),
                PartPose.offset(0.0F, 20.0F, -1.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 12).mirror()
                        .addBox(-2.0F, -1.0F, 0.0F, 4, 2, 5),
                PartPose.offsetAndRotation(0.0F, 20.0F, -1.0F, -0.4833219F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(13, 26).mirror()
                        .addBox(-1.0F, 0.0F, 0.0F, 1, 4, 1),
                PartPose.offsetAndRotation(-2.0F, 20.0F, -1.0F, -0.3346075F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(18, 26).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 1, 4, 1),
                PartPose.offsetAndRotation(2.0F, 20.0F, -1.0F, -0.3346075F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().mirror()
                        .texOffs(0, 20).addBox(-2.0F, -1.0F, -2.0F, 2, 2, 3)
                        .texOffs(0, 26).addBox(-3.0F, 1.0F, -4.0F, 3, 0, 3),
                PartPose.offset(-2.0F, 23.0F, 3.0F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().mirror()
                        .texOffs(11, 20).addBox(0.0F, -1.0F, -2.0F, 2, 2, 3)
                        .texOffs(0, 26).addBox(0.0F, 1.0F, -4.0F, 3, 0, 3),
                PartPose.offset(2.0F, 23.0F, 3.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createBabyLayer() {
        return createBodyLayer().apply(new BabyModelTransform(
                true, 10.0F, 0.0F, 2.0F, 2.0F, 24.0F, Set.of("head")));
    }

    @Override
    public void setupAnim(ToadRenderState state) {
        resetPose();
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        if (state.sitting) {
            legLeft.xRot = -Mth.PI / 8.0F;
            legRight.xRot = legLeft.xRot;
            return;
        }

        float movementBias = state.walkAnimationSpeed > 0.1F || state.walkAnimationSpeed < -0.1F
                ? 0.5F : 0.0F;
        legLeft.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
                * 1.4F * state.walkAnimationSpeed + movementBias;
        legRight.xRot = legLeft.xRot;
        armLeft.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI)
                * 1.4F * state.walkAnimationSpeed;
        armRight.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
                * 1.4F * state.walkAnimationSpeed;
    }

    public static class ToadRenderState extends LivingEntityRenderState {
        public boolean sitting;
        public int skinColor;
        public int tintColor = -1;
    }
}
