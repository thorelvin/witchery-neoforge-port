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

/**
 * Cat familiar model. The original Witchery renderer used vanilla 1.7's
 * ModelOcelot with Witchery's blackcat.png painted for that UV layout.
 */
public class CatFamiliarModel extends EntityModel<CatFamiliarModel.CatFamiliarRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "familiar_cat"), "main");
    public static final ModelLayerLocation BABY_LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "familiar_cat"), "baby");
    private static final float BODY_X_ROT = Mth.HALF_PI;
    private static final float TAIL_1_X_ROT = 0.9F;
    private static final float TAIL_2_X_ROT = 1.7278761F;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;
    private final ModelPart tail1;
    private final ModelPart tail2;

    public static class CatFamiliarRenderState extends LivingEntityRenderState {
        public boolean sitting;
        public boolean crouching;
        public boolean sprinting;
        public boolean tamed;
    }

    public CatFamiliarModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 5.0F)
                        .texOffs(0, 24).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 2.0F, 2.0F)
                        .texOffs(0, 10).addBox(-2.0F, -3.0F, 0.0F, 1.0F, 1.0F, 2.0F)
                        .texOffs(6, 10).addBox(1.0F, -3.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -9.0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.0F, 3.0F, -8.0F, 4.0F, 16.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, -10.0F, BODY_X_ROT, 0.0F, 0.0F));

        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(1.2F, 13.8F, -5.0F));

        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-1.2F, 13.8F, -5.0F));

        root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(8, 13).addBox(-1.0F, 0.0F, 1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(1.1F, 18.0F, 5.0F));

        root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(8, 13).addBox(-1.0F, 0.0F, 1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-1.1F, 18.0F, 5.0F));

        root.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 15).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 15.0F, 8.0F, TAIL_1_X_ROT, 0.0F, 0.0F));

        root.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(4, 15).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 14.0F, TAIL_2_X_ROT, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createBabyLayer() {
        return createBodyLayer().apply(new BabyModelTransform(
                true, 10.0F, 4.0F, 2.0F, 2.0F, 24.0F, Set.of("head")));
    }

    @Override
    public void setupAnim(CatFamiliarRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        resetPose();

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        if (state.crouching) {
            applyCrouchingPose(state.isBaby);
        } else if (state.sprinting) {
            applySprintingPose(state.isBaby);
        } else if (state.sitting) {
            applySittingPose(state.isBaby);
            return;
        }

        legBackLeft.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        if (state.sprinting) {
            legBackRight.xRot = Mth.cos(limbSwing * 0.6662F + 0.3F) * limbSwingAmount;
            legFrontLeft.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI + 0.3F) * limbSwingAmount;
            legFrontRight.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount;
            tail2.xRot = TAIL_2_X_ROT + Mth.PI / 10.0F * Mth.cos(limbSwing) * limbSwingAmount;
        } else {
            legBackRight.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount;
            legFrontLeft.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount;
            legFrontRight.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
            float tailAmplitude = state.crouching ? 0.47123894F : Mth.PI * 0.25F;
            tail2.xRot = TAIL_2_X_ROT + tailAmplitude * Mth.cos(limbSwing) * limbSwingAmount;
        }
    }

    private void applyCrouchingPose(boolean baby) {
        float bodyScale = baby ? 0.5F : 1.0F;
        float headScale = baby ? 0.75F : 1.0F;
        body.y += bodyScale;
        head.y += 2.0F * headScale;
        tail1.y += bodyScale;
        tail2.y -= 4.0F * bodyScale;
        tail2.z += 2.0F * bodyScale;
        tail1.xRot = Mth.HALF_PI;
        tail2.xRot = Mth.HALF_PI;
    }

    private void applySprintingPose(boolean baby) {
        float bodyScale = baby ? 0.5F : 1.0F;
        tail2.y = tail1.y;
        tail2.z += 2.0F * bodyScale;
        tail1.xRot = Mth.HALF_PI;
        tail2.xRot = Mth.HALF_PI;
    }

    private void applySittingPose(boolean baby) {
        float bodyScale = baby ? 0.5F : 1.0F;
        float headScale = baby ? 0.75F : 1.0F;
        body.xRot = Mth.PI / 4.0F;
        body.y -= 4.0F * bodyScale;
        body.z += 5.0F * bodyScale;
        head.y -= 3.3F * headScale;
        head.z += headScale;
        tail1.y += 8.0F * bodyScale;
        tail1.z -= 2.0F * bodyScale;
        tail2.y += 2.0F * bodyScale;
        tail2.z -= 0.8F * bodyScale;
        tail1.xRot = TAIL_2_X_ROT;
        tail2.xRot = 2.670354F;
        legFrontLeft.xRot = -Mth.PI / 20.0F;
        legFrontLeft.y += 2.0F * bodyScale;
        legFrontLeft.z -= 2.0F * bodyScale;
        legFrontRight.xRot = -Mth.PI / 20.0F;
        legFrontRight.y += 2.0F * bodyScale;
        legFrontRight.z -= 2.0F * bodyScale;
        legBackLeft.xRot = -Mth.HALF_PI;
        legBackLeft.y += 3.0F * bodyScale;
        legBackLeft.z -= 4.0F * bodyScale;
        legBackRight.xRot = -Mth.HALF_PI;
        legBackRight.y += 3.0F * bodyScale;
        legBackRight.z -= 4.0F * bodyScale;
    }
}
