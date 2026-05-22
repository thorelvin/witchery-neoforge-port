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
 * Cat familiar model. The original Witchery renderer used vanilla 1.7's
 * ModelOcelot with Witchery's blackcat.png painted for that UV layout.
 */
public class CatFamiliarModel extends EntityModel<CatFamiliarModel.CatFamiliarRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "familiar_cat"), "main");
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
                        .texOffs(0, 24).addBox(-1.5F, -0.001F, -4.0F, 3.0F, 2.0F, 2.0F)
                        .texOffs(0, 10).addBox(-2.0F, -3.0F, 0.0F, 1.0F, 1.0F, 2.0F)
                        .texOffs(6, 10).addBox(1.0F, -3.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -9.0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.0F, 3.0F, -8.0F, 4.0F, 16.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, -10.0F, BODY_X_ROT, 0.0F, 0.0F));

        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(1.2F, 14.1F, -5.0F));

        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-1.2F, 14.1F, -5.0F));

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

    @Override
    public void setupAnim(CatFamiliarRenderState state) {
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        resetToStandingPose();

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        if (state.sitting) {
            applySittingPose();
            return;
        }

        legBackLeft.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        legBackRight.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount;
        legFrontLeft.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount;
        legFrontRight.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        tail2.xRot = TAIL_2_X_ROT + Mth.PI * 0.25F * Mth.cos(limbSwing) * limbSwingAmount;
    }

    private void resetToStandingPose() {
        body.xRot = BODY_X_ROT;
        body.y = 12.0F;
        body.z = -10.0F;
        head.y = 15.0F;
        head.z = -9.0F;
        tail1.y = 15.0F;
        tail1.z = 8.0F;
        tail1.xRot = TAIL_1_X_ROT;
        tail1.yRot = 0.0F;
        tail2.y = 20.0F;
        tail2.z = 14.0F;
        tail2.xRot = TAIL_2_X_ROT;
        tail2.yRot = 0.0F;
        legFrontLeft.y = 14.1F;
        legFrontLeft.z = -5.0F;
        legFrontRight.y = 14.1F;
        legFrontRight.z = -5.0F;
        legBackLeft.y = 18.0F;
        legBackLeft.z = 5.0F;
        legBackRight.y = 18.0F;
        legBackRight.z = 5.0F;
        legFrontLeft.xRot = 0.0F;
        legFrontRight.xRot = 0.0F;
        legBackLeft.xRot = 0.0F;
        legBackRight.xRot = 0.0F;
    }

    private void applySittingPose() {
        body.xRot = Mth.PI / 4.0F;
        body.y -= 4.0F;
        body.z += 5.0F;
        head.y -= 3.3F;
        head.z += 1.0F;
        tail1.y += 8.0F;
        tail1.z -= 2.0F;
        tail2.y += 2.0F;
        tail2.z -= 0.8F;
        tail1.xRot = TAIL_2_X_ROT;
        tail2.xRot = 2.670354F;
        legFrontLeft.xRot = -Mth.PI / 20.0F;
        legFrontLeft.y = 16.1F;
        legFrontLeft.z = -7.0F;
        legFrontRight.xRot = -Mth.PI / 20.0F;
        legFrontRight.y = 16.1F;
        legFrontRight.z = -7.0F;
        legBackLeft.xRot = -Mth.HALF_PI;
        legBackLeft.y = 21.0F;
        legBackLeft.z = 1.0F;
        legBackRight.xRot = -Mth.HALF_PI;
        legBackRight.y = 21.0F;
        legBackRight.z = 1.0F;
    }
}
