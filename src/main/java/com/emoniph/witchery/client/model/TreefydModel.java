package com.emoniph.witchery.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Treefyd — plant-spirit golem. 64×32 texture.
 * Ported 1:1 from original ModelTreefyd (witchery-1.7.2-0.19.0).
 * Shape: screaming flower head, thin stalk body, diagonal leaf planes, 4 root legs.
 */
public class TreefydModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "treefyd"), "main");

    private final ModelPart head;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;

    public TreefydModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Screaming flower head: face cube + thin petal disc + thin tongue
        // 1-unit thickness replaces original 0-depth/width planes (0-size boxes crash baking in 1.21.11)
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0, 3, 0));
        head.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2, -4, -2, 4, 4, 4),
                PartPose.ZERO);
        head.addOrReplaceChild("petals",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5, -7, -0.5f, 10, 10, 1),
                PartPose.ZERO);
        head.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(25, 18).addBox(-0.5f, -3, -6, 1, 10, 4),
                PartPose.ZERO);

        // Thin central stalk
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 14).addBox(-1, 0, -1, 2, 16, 2),
                PartPose.offset(0, 3, 0));

        // Diagonal leaf planes crossing at 45° — 1-unit thin (0-size boxes crash baking)
        // texOffs 40 would need u up to 66 (overflow on 64px texture) — start at 38 instead
        root.addOrReplaceChild("leaves_v",
                CubeListBuilder.create().texOffs(38, 6).addBox(-0.5f, 0, -6, 1, 14, 12),
                PartPose.offsetAndRotation(0, 6, 0, 0, Mth.PI / 4, 0));
        root.addOrReplaceChild("leaves_h",
                CubeListBuilder.create().texOffs(38, 0).addBox(-6, 0, -0.5f, 12, 14, 1),
                PartPose.offsetAndRotation(0, 6, 0, 0, Mth.PI / 4, 0));

        // Small base plate
        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(15, 6).addBox(0, 0, 0, 6, 1, 6),
                PartPose.offset(-3, 19, -3));

        // Four root legs — bottom at y=20+4=24 (floor reference)
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(-2, 20, 4));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(2, 20, 4));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(-2, 20, -4));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(2, 20, -4));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float limbSwing       = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Alternate diagonal pairs of legs (1+4 vs 2+3)
        float swing = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        leg1.xRot =  swing;
        leg4.xRot =  swing;
        leg2.xRot = -swing;
        leg3.xRot = -swing;
    }
}
