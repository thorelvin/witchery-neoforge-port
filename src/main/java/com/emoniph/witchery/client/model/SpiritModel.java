package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.SpiritEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

/**
 * Spirit model — ported from ModelSpirit (32×32).
 * Small orb shape made of 5 layered boxes. Rendered at 0.5x scale in SpiritRenderer.
 */
public class SpiritModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "spirit"), "main");
    private final ModelPart body;

    public SpiritModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create(), PartPose.offset(0f, 21f, 0f));

        body.addOrReplaceChild("shape1",
                CubeListBuilder.create().texOffs(2, 5).addBox(-2.5f, -2f, -2.5f, 5, 1, 5),
                PartPose.ZERO);
        body.addOrReplaceChild("shape2",
                CubeListBuilder.create().texOffs(2, 21).addBox(-2.5f, 1f, -2.5f, 5, 1, 5),
                PartPose.ZERO);
        body.addOrReplaceChild("shape3",
                CubeListBuilder.create().texOffs(0, 12).addBox(-3f, -1f, -3f, 6, 2, 6),
                PartPose.ZERO);
        body.addOrReplaceChild("shape4",
                CubeListBuilder.create().texOffs(6, 0).addBox(-1.5f, -3f, -1.5f, 3, 1, 3),
                PartPose.ZERO);
        body.addOrReplaceChild("shape5",
                CubeListBuilder.create().texOffs(6, 28).addBox(-1.5f, 2f, -1.5f, 3, 1, 3),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

        @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        body.y = 21f + (float) Math.sin(ageInTicks * 0.05f) * 0.5f;
    }
}
