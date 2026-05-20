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

public class HornedHuntsmanModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "horned_huntsman"), "main");

    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public HornedHuntsmanModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftLeg = body.getChild("left_leg");
        this.rightLeg = body.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 22).addBox(-6.0F, 0.0F, -2.5F, 12.0F, 16.0F, 5.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) (-Math.PI / 12.0), 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 14.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.87266463F, 0.0F, 0.0F));

        head.addOrReplaceChild("antler_right",
                CubeListBuilder.create().texOffs(45, 0).addBox(-12.0F, -18.0F, 0.0F, 11.0F, 13.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5009095F, 0.0F, 0.0F));

        head.addOrReplaceChild("antler_left",
                CubeListBuilder.create().texOffs(45, 0).mirror().addBox(1.0F, -18.0F, 0.0F, 11.0F, 13.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5009095F, 0.0F, 0.0F));

        body.addOrReplaceChild("moss",
                CubeListBuilder.create().texOffs(0, 43).addBox(-6.0F, 16.0F, -2.5F, 12.0F, 5.0F, 5.0F),
                PartPose.ZERO);

        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(34, 22).addBox(-5.0F, -2.0F, -2.5F, 5.0F, 19.0F, 5.0F),
                PartPose.offset(-6.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("right_claws",
                CubeListBuilder.create().texOffs(54, 22).addBox(-5.0F, 17.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.ZERO);

        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(34, 22).mirror().addBox(0.0F, -2.0F, -2.5F, 5.0F, 19.0F, 5.0F),
                PartPose.offset(6.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("left_claws",
                CubeListBuilder.create().texOffs(54, 22).mirror().addBox(0.0F, 17.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.ZERO);

        PartDefinition rightLeg = body.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(34, 46).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F),
                PartPose.offset(-3.0F, 16.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_lower_leg",
                CubeListBuilder.create().texOffs(54, 41).mirror().addBox(-2.5F, 7.0F, 2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.ZERO);

        PartDefinition leftLeg = body.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(34, 46).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F),
                PartPose.offset(3.0F, 16.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_lower_leg",
                CubeListBuilder.create().texOffs(54, 41).addBox(-2.5F, 7.0F, 2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD + 0.4365F;
        neck.xRot = (float) (-Math.PI / 12.0);
        neck.yRot = 0.0F;

        float wave = triangleWave(state.walkAnimationPos, 13.0F) * state.walkAnimationSpeed;
        leftLeg.xRot = wave * -1.3F - (float) (Math.PI / 9.0);
        rightLeg.xRot = wave * 1.3F - (float) (Math.PI / 9.0);
        leftArm.xRot = wave * -0.3F;
        rightArm.xRot = -leftArm.xRot;
    }

    private static float triangleWave(float angle, float limit) {
        return (Math.abs(angle % limit - limit * 0.5F) - limit * 0.25F) / (limit * 0.25F);
    }
}
