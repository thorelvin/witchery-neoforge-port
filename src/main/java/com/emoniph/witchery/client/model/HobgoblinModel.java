package com.emoniph.witchery.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Resurrected-style goblin body used by Hobgoblins.
 * Matches the old Witchery goblin UV layout instead of stretching the texture over a zombie.
 */
public class HobgoblinModel<S extends HumanoidRenderState> extends HumanoidModel<S> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "hobgoblin"), "main");

    public HobgoblinModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                        .texOffs(34, 3).addBox(-0.5F, -6.0F, -5.0F, 1, 3, 1)
                        .texOffs(34, 0).addBox(-0.5F, -5.0F, -6.0F, 1, 1, 1)
                        .texOffs(33, 9).addBox(-0.5F, -4.0F, -7.0F, 1, 2, 2)
                        .texOffs(46, 0).addBox(6.0F, -7.0F, 0.0F, 2, 2, 1)
                        .texOffs(46, 0).addBox(-8.0F, -7.0F, 0.0F, 2, 2, 1)
                        .texOffs(39, 0).addBox(4.0F, -7.0F, 0.0F, 2, 3, 1)
                        .texOffs(39, 0).addBox(-6.0F, -7.0F, 0.0F, 2, 3, 1),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        root.addOrReplaceChild("hat",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 7, 4),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -3.0F, -2.0F, 4, 12, 4),
                PartPose.offset(-5.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -3.0F, -2.0F, 4, 12, 4),
                PartPose.offset(5.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(-2.0F, 18.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(2.0F, 18.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        this.head.y = 11.0F;
        this.hat.y = 11.0F;
        this.body.y = 11.0F;
        this.rightArm.y = 12.0F;
        this.leftArm.y = 12.0F;
        this.rightLeg.y = 18.0F;
        this.leftLeg.y = 18.0F;
    }
}
