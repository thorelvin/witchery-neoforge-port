package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Hellhound model — ported from ModelHellhound (1.7.10-0.24.1), which is the vanilla
 * wolf geometry (head with ears + snout, mane collar, body, tail, four legs) on a 64×32
 * sheet. Standard quadruped gait; head tracks the look target.
 */
public class HellhoundModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "hellhound"), "main");

    private final ModelPart head;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart tail;

    public HellhoundModel(ModelPart root) {
        super(root);
        this.head          = root.getChild("head");
        this.legBackLeft   = root.getChild("leg_back_left");
        this.legBackRight  = root.getChild("leg_back_right");
        this.legFrontLeft  = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.tail          = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head — skull, two ears, snout (single multi-box part, as in the source)
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -2.0F, 6, 6, 4)
                        .texOffs(16, 14).addBox(-3.0F, -5.0F, 1.0F, 2, 2, 1)   // left ear
                        .texOffs(16, 14).addBox(1.0F, -5.0F, 1.0F, 2, 2, 1)    // right ear
                        .texOffs(0, 10).addBox(-1.5F, 0.0F, -5.0F, 3, 3, 4),   // snout
                PartPose.offsetAndRotation(-1.0F, 10.6F, -7.5F, 0.22759093F, 0.0F, 0.0F));

        // Body — slightly hunched (source xRot 1.1838568)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(18, 17).addBox(-4.0F, -2.0F, -3.0F, 6, 9, 6),
                PartPose.offsetAndRotation(0.0F, 12.5F, 2.0F, 1.1838568F, 0.0F, 0.0F));

        // Mane / collar around the neck
        root.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(22, 0).addBox(-4.0F, -3.0F, -3.0F, 8, 8, 9),
                PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, Mth.HALF_PI, 0.0F, 0.0F));

        // Tail
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(9, 19).addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2),
                PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, 0.59184116F, 0.0F, 0.0F));

        // Legs (2×8×2) — back pair at z=7, front pair at z=-4
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 19).addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2);
        root.addOrReplaceChild("leg_back_left",   leg, PartPose.offset(-2.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("leg_back_right",  leg, PartPose.offset(0.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("leg_front_left",  leg, PartPose.offset(-2.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("leg_front_right", leg, PartPose.offset(0.5F, 16.0F, -4.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float swing  = state.walkAnimationPos;
        float amount = Math.min(state.walkAnimationSpeed, 1.0F);

        head.xRot = 0.22759093F + state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;

        // Diagonal gait: back-left + front-right move together; back-right + front-left opposite.
        float phase = Mth.cos(swing * 0.6662F) * 1.4F * amount;
        float anti  = Mth.cos(swing * 0.6662F + Mth.PI) * 1.4F * amount;
        legBackLeft.xRot   = phase;
        legFrontRight.xRot = phase;
        legBackRight.xRot  = anti;
        legFrontLeft.xRot  = anti;

        tail.xRot = 0.59184116F + Mth.sin(state.ageInTicks * 0.1F) * 0.12F;
    }
}
