package com.emoniph.witchery.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class SpinningWheelModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "spinning_wheel"), "main");

    private final ModelPart thread;
    private final ModelPart wheel;

    public SpinningWheelModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull);
        this.thread = root.getChild("thread");
        this.wheel = root.getChild("wheel");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("seat",
                cube(0, 0).addBox(-2.0F, -1.0F, -7.0F, 4, 1, 14),
                PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.2602503F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_right",
                cube(32, 0).addBox(-1.0F, 0.0F, 0.0F, 1, 9, 1),
                PartPose.offsetAndRotation(-1.0F, 16.0F, 5.0F, 0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("leg_back_left",
                cube(32, 0).addBox(0.0F, 0.0F, 0.0F, 1, 9, 1),
                PartPose.offsetAndRotation(1.0F, 16.0F, 5.0F, 0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("leg_front_right",
                cube(0, 6).addBox(-1.0F, 0.0F, 0.0F, 1, 6, 1),
                PartPose.offsetAndRotation(-1.0F, 19.0F, -6.0F, -0.1745329F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("leg_front_left",
                cube(0, 6).addBox(0.0F, 0.0F, 0.0F, 1, 6, 1),
                PartPose.offsetAndRotation(1.0F, 19.0F, -6.0F, -0.1745329F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("thread",
                cube(23, 0).addBox(-1.0F, -3.0F, -1.0F, 2, 3, 2),
                PartPose.offset(0.0F, 12.0F, 5.0F));
        root.addOrReplaceChild("thread_pole",
                cube(9, 7).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1),
                PartPose.offset(0.0F, 12.0F, 5.0F));
        root.addOrReplaceChild("arm_right",
                cube(28, 6).addBox(-0.5F, -7.0F, -0.5F, 1, 7, 1),
                PartPose.offsetAndRotation(-1.0F, 18.0F, -2.0F, 0.2268928F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_left",
                cube(28, 6).addBox(-0.5F, -7.0F, -0.5F, 1, 7, 1),
                PartPose.offsetAndRotation(1.0F, 18.0F, -2.0F, 0.2268928F, 0.0F, 0.0F));

        root.addOrReplaceChild("wheel", CubeListBuilder.create().mirror()
                        .texOffs(0, -6).addBox(0.0F, -3.0F, -3.0F, 0, 6, 6)
                        .texOffs(0, 7).addBox(-0.5F, -4.0F, -3.0F, 1, 1, 6)
                        .texOffs(0, 7).addBox(-0.5F, 3.0F, -3.0F, 1, 1, 6)
                        .texOffs(23, 5).addBox(-0.5F, -4.0F, 3.0F, 1, 8, 1)
                        .texOffs(23, 5).addBox(-0.5F, -4.0F, -4.0F, 1, 8, 1),
                PartPose.offset(0.0F, 12.0F, -3.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setupAnimation(boolean spinning, long elapsedMillis) {
        long ticks = elapsedMillis / 25L;
        wheel.xRot = spinning ? (float) (-(ticks / 3L) % 360L) : 0.0F;
        thread.yRot = spinning ? (float) (ticks / 2L % 360L) : 0.0F;
    }

    ModelPart wheel() {
        return wheel;
    }

    ModelPart thread() {
        return thread;
    }

    private static CubeListBuilder cube(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }
}
