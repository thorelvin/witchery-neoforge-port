package com.emoniph.witchery.client.model;

import com.emoniph.witchery.entity.BroomEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Broom model — faithfully ported from ModelBroom.java (1.7.10 original).
 * 32×32 texture. Handle runs along Z axis after X-rotation.
 * Nine individual bristle strands fan out from the rear of the handle.
 */
public class BroomModel extends EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "broom"), "main");
    private final ModelPart handle;
    private final ModelPart bristle1, bristle2, bristle3, bristle4, bristle5,
                            bristle6, bristle7, bristle8, bristle9;

    public BroomModel(ModelPart root) {
        super(root);
        this.handle   = root.getChild("handle");
        this.bristle1 = root.getChild("bristle1");
        this.bristle2 = root.getChild("bristle2");
        this.bristle3 = root.getChild("bristle3");
        this.bristle4 = root.getChild("bristle4");
        this.bristle5 = root.getChild("bristle5");
        this.bristle6 = root.getChild("bristle6");
        this.bristle7 = root.getChild("bristle7");
        this.bristle8 = root.getChild("bristle8");
        this.bristle9 = root.getChild("bristle9");
    }

    public void setHandleVisible(boolean visible) {
        this.handle.visible = visible;
    }

    public void setBristlesVisible(boolean visible) {
        this.bristle1.visible = visible;
        this.bristle2.visible = visible;
        this.bristle3.visible = visible;
        this.bristle4.visible = visible;
        this.bristle5.visible = visible;
        this.bristle6.visible = visible;
        this.bristle7.visible = visible;
        this.bristle8.visible = visible;
        this.bristle9.visible = visible;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Handle: 2×24×2 stick, rotated 90° on X so it lies along Z
        root.addOrReplaceChild("handle",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1f, -10f, -1f, 2, 24, 2),
                PartPose.offsetAndRotation(0f, 11f, -5f, (float) Math.PI / 2f, 0f, 0f));

        // Nine thin bristle strands (1×1 cross-section) fanning from the rear of the handle.
        // Positions and rotations taken directly from ModelBroom.java (1.7.10 original).
        root.addOrReplaceChild("bristle1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, 0f, 1, 1, 10),
                PartPose.offsetAndRotation(-1f, 10f, 9f, 0.1858931f, -0.1487144f, 0f));
        root.addOrReplaceChild("bristle2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, 0f, 1, 1, 10),
                PartPose.offsetAndRotation(1f, 12f, 9f, -0.1487144f, 0.1858931f, 0f));
        root.addOrReplaceChild("bristle3",
                CubeListBuilder.create().texOffs(0, 12).addBox(-0.5f, -0.5f, 0f, 1, 1, 9),
                PartPose.offsetAndRotation(1f, 10f, 9f, 0.2230717f, 0.1858931f, 0f));
        root.addOrReplaceChild("bristle4",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, 0f, 1, 1, 10),
                PartPose.offsetAndRotation(0f, 10f, 9f, 0.2230717f, 0.0743572f, 0f));
        root.addOrReplaceChild("bristle5",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, 0f, 1, 1, 10),
                PartPose.offsetAndRotation(-1f, 12f, 9f, -0.2230717f, -0.1487144f, 0f));
        root.addOrReplaceChild("bristle6",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, 0f, 1, 1, 10),
                PartPose.offsetAndRotation(0f, 11f, 9f, -0.0371786f, 0.0743572f, 0f));
        root.addOrReplaceChild("bristle7",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, 0f, 1, 1, 10),
                PartPose.offsetAndRotation(1f, 11f, 9f, -0.0371786f, 0.2230717f, 0f));
        root.addOrReplaceChild("bristle8",
                CubeListBuilder.create().texOffs(0, 12).addBox(-0.5f, -0.5f, 0f, 1, 1, 9),
                PartPose.offsetAndRotation(-1f, 11f, 9f, -0.0743572f, -0.1487144f, 0f));
        root.addOrReplaceChild("bristle9",
                CubeListBuilder.create().texOffs(0, 12).addBox(-0.5333f, -0.5f, 0f, 1, 1, 9),
                PartPose.offsetAndRotation(0f, 12f, 9f, -0.1858931f, 0f, 0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

        @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;
        float speed = limbSwingAmount;
        float sway = Mth.sin(ageInTicks * 0.05f) * 0.03f;
        handle.zRot = sway;
        float bristleSway = Mth.sin(ageInTicks * 0.08f + 0.5f) * 0.04f * (1f + speed * 2f);
        bristle1.zRot = bristleSway;
        bristle2.zRot = -bristleSway;
        bristle3.zRot = bristleSway * 0.7f;
        bristle4.zRot = bristleSway * 0.5f;
        bristle5.zRot = -bristleSway * 0.7f;
        bristle6.zRot = bristleSway * 0.3f;
        bristle7.zRot = -bristleSway * 0.4f;
        bristle8.zRot = bristleSway * 0.6f;
        bristle9.zRot = -bristleSway * 0.5f;
    }
}
