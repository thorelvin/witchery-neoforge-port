package com.emoniph.witchery.client.model;

import com.emoniph.witchery.client.renderer.WolfmanRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Wolfman model — ported from ModelWolfman (1.7.10-0.24.1).
 * 64×64 texture. Double-jointed hind legs, tail, wolf head with ears and snout.
 */
public class WolfmanModel extends EntityModel<WolfmanRenderer.WolfmanRenderState> {

    // NOTE: must NOT be "wolfman/main" — that is already taken by WerewolfModel (player render layer).
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "wolfman_entity"), "main");

    private final ModelPart headMain;
    private final ModelPart bodyUpper;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeftUpper;
    private final ModelPart legLeftLower;
    private final ModelPart legRightUpper;
    private final ModelPart legRightLower;
    private final ModelPart tail;

    public WolfmanModel(ModelPart root) {
        super(root);
        this.headMain     = root.getChild("head_main");
        this.bodyUpper    = root.getChild("body_upper");
        this.armLeft      = root.getChild("arm_left");
        this.armRight     = root.getChild("arm_right");
        this.legLeftUpper = root.getChild("leg_left_upper");
        this.legRightUpper= root.getChild("leg_right_upper");
        // Children accessed via parents for animation
        ModelPart bodyUpperChild = this.bodyUpper;
        this.tail          = bodyUpperChild.getChild("tail");
        this.legLeftLower  = this.legLeftUpper.getChild("leg_left_lower");
        this.legRightLower = this.legRightUpper.getChild("leg_right_lower");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head — wolf snout, ears, main skull (all in one builder matching 1.7.10 ModelRenderer multi-box)
        root.addOrReplaceChild("head_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3, -6, -2, 6, 6, 4, new CubeDeformation(0.05f))
                        .texOffs(16, 14).addBox(-3, -8, 1, 2, 2, 1)   // left ear
                        .texOffs(16, 14).addBox(1, -8, 1, 2, 2, 1)    // right ear
                        .texOffs(0, 10).addBox(-1.5f, -3.1f, -5, 3, 3, 4), // snout
                PartPose.offset(0, 0, -2));

        // Body — upper torso leans forward; lower torso and tail are children
        PartDefinition bodyUpper = root.addOrReplaceChild("body_upper",
                CubeListBuilder.create().texOffs(0, 35).addBox(-5, 0, -3.9f, 10, 7, 8),
                PartPose.offsetAndRotation(0, -0.1f, -2, 0.4098033f, 0, 0));

        bodyUpper.addOrReplaceChild("body_lower",
                CubeListBuilder.create().texOffs(3, 50).addBox(-4, 2, -2.3f, 8, 7, 5),
                PartPose.offset(0, 5, -1.5f));

        bodyUpper.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(55, 52).addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offsetAndRotation(0, 11.9f, 3.6f, 0.59184116f, 0, 0));

        // Arms (attached to body, not children)
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(38, 46).mirror().addBox(-1, -2, -2, 4, 14, 4),
                PartPose.offset(6, 2, 0));

        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(38, 46).addBox(-3, -2, -2, 4, 14, 4),
                PartPose.offset(-5.8f, 2, 0));

        // Hind legs — double-jointed: upper at hip, lower as child bending backwards
        PartDefinition legLeftUpper = root.addOrReplaceChild("leg_left_upper",
                CubeListBuilder.create().texOffs(38, 0).mirror().addBox(-2, 0, -2, 4, 7, 4),
                PartPose.offsetAndRotation(2, 12, 0, -0.4098033f, 0, 0));
        legLeftUpper.addOrReplaceChild("leg_left_lower",
                CubeListBuilder.create().texOffs(38, 13).addBox(-2, 3.5f, 2, 4, 8, 4),
                PartPose.ZERO);

        PartDefinition legRightUpper = root.addOrReplaceChild("leg_right_upper",
                CubeListBuilder.create().texOffs(38, 0).addBox(-2, 0, -2, 4, 7, 4),
                PartPose.offsetAndRotation(-2, 12, 0, -0.4098033f, 0, 0));
        legRightUpper.addOrReplaceChild("leg_right_lower",
                CubeListBuilder.create().texOffs(38, 13).addBox(-2, 3.5f, 2, 4, 8, 4),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    @Override
    public void setupAnim(WolfmanRenderer.WolfmanRenderState state) {
        float limbSwing  = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float age        = state.ageInTicks;
        float yaw        = state.yRot * Mth.DEG_TO_RAD;
        float pitch      = state.xRot * Mth.DEG_TO_RAD;

        // Head tracks target
        headMain.yRot = yaw;
        headMain.xRot = pitch;

        // Arms swing opposite to each other and to the legs
        armLeft.xRot  =  Mth.cos(limbSwing * 0.6662f + Mth.PI) * 2.0f * limbAmount * 0.5f;
        armRight.xRot =  Mth.cos(limbSwing * 0.6662f)           * 2.0f * limbAmount * 0.5f;
        armLeft.zRot  =  0;
        armRight.zRot =  0;

        // Hind leg upper swing — more aggressive than a biped (1.4× scale)
        float legSwingL = Mth.cos(limbSwing * 0.6662f)           * 1.4f * limbAmount;
        float legSwingR = Mth.cos(limbSwing * 0.6662f + Mth.PI)  * 1.4f * limbAmount;
        legLeftUpper.xRot  = -0.4098033f + legSwingL;
        legRightUpper.xRot = -0.4098033f + legSwingR;

        // Lower leg bends inversely (digitigrade gait)
        legLeftLower.xRot  = Math.max(0, -legSwingL * 0.6f);
        legRightLower.xRot = Math.max(0, -legSwingR * 0.6f);

        // Tail wags idly
        tail.xRot = 0.59184116f + Mth.sin(age * 0.1f) * 0.15f;

        // Attack animation — arms lunge forward
        if (state.attackTime > 0) {
            float t = state.attackTime / 10.0f;
            armLeft.xRot  -= Mth.PI * 0.5f * t;
            armRight.xRot -= Mth.PI * 0.5f * t;
        }
    }
}
