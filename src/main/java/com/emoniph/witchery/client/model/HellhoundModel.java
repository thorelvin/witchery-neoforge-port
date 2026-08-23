package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Exact geometry and live pose contract from Witchery's 1.7.10 ModelHellhound. */
public class HellhoundModel extends EntityModel<HellhoundModel.HellhoundRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "hellhound"), "main");

    private static final float BODY_X_ROT = 1.1838568F;
    private static final float MANE_X_ROT = Mth.HALF_PI;
    private static final float TAIL_X_ROT = 0.59184116F;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart mane;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart tail;

    public HellhoundModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.mane = root.getChild("mane");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -2.0F, 6, 6, 4)
                        .texOffs(16, 14).addBox(-3.0F, -5.0F, 1.0F, 2, 2, 1)
                        .texOffs(16, 14).addBox(1.0F, -5.0F, 1.0F, 2, 2, 1)
                        .texOffs(0, 10).addBox(-1.5F, 0.0F, -5.0F, 3, 3, 4),
                LegacyModelTransforms.renderWithRotation(
                        -1.0F, 10.6F, -7.5F, 0.22759093F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 17)
                        .addBox(-4.0F, -2.0F, -3.0F, 6, 9, 6),
                LegacyModelTransforms.render(0.0F, 12.5F, 2.0F, BODY_X_ROT, 0.0F, 0.0F));
        root.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(22, 0)
                        .addBox(-4.0F, -3.0F, -3.0F, 8, 8, 9),
                LegacyModelTransforms.render(-1.0F, 14.0F, -3.0F, MANE_X_ROT, 0.0F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(9, 19)
                        .addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2),
                LegacyModelTransforms.renderWithRotation(-1.0F, 12.0F, 8.0F, 0.0F, 0.0F, 0.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 19)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2);
        root.addOrReplaceChild("leg_back_left", leg,
                LegacyModelTransforms.render(-2.5F, 16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_right", leg,
                LegacyModelTransforms.render(0.5F, 16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_left", leg,
                LegacyModelTransforms.render(-2.5F, 16.0F, -4.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_right", leg,
                LegacyModelTransforms.render(0.5F, 16.0F, -4.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HellhoundRenderState state) {
        super.setupAnim(state);

        float amount = state.walkAnimationSpeed;
        float phase = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * amount;
        float antiPhase = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI) * 1.4F * amount;

        body.setPos(0.0F, 12.5F, 2.0F);
        body.setRotation(BODY_X_ROT, 0.0F, state.bodyRoll);
        mane.setPos(-1.0F, 14.0F, -3.0F);
        mane.setRotation(MANE_X_ROT, 0.0F, state.maneRoll);
        legBackLeft.setPos(-2.5F, 16.0F, 7.0F);
        legBackRight.setPos(0.5F, 16.0F, 7.0F);
        legFrontLeft.setPos(-2.5F, 16.0F, -4.0F);
        legFrontRight.setPos(0.5F, 16.0F, -4.0F);
        legBackLeft.xRot = phase;
        legBackRight.xRot = antiPhase;
        legFrontLeft.xRot = antiPhase;
        legFrontRight.xRot = phase;

        float headXRot = state.xRot * Mth.DEG_TO_RAD + (state.converting ? 0.5F : 0.15F);
        LegacyModelTransforms.setRenderWithRotation(
                head, headXRot, state.yRot * Mth.DEG_TO_RAD, state.headRoll);

        float tailXRot = state.converting ? 2.0F : TAIL_X_ROT;
        if (amount > 0.1F) {
            tailXRot += 1.5F * amount - 0.1F;
        }
        tail.setPos(-1.0F, 13.0F, 8.0F);
        LegacyModelTransforms.setRenderWithRotation(tail, tailXRot, 0.0F, state.tailRoll);
    }

    public static class HellhoundRenderState extends LivingEntityRenderState {
        public boolean converting;
        public float headRoll;
        public float maneRoll;
        public float bodyRoll;
        public float tailRoll;
    }
}
