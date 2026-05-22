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

public class ParasyticLouseModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "parasytic_louse"), "main");

    private static final int[][] BODY_SIZES = new int[][]{
            {3, 2, 2}, {4, 3, 2}, {4, 3, 2}, {3, 3, 3}, {2, 2, 3}, {2, 1, 2}, {1, 1, 2}
    };
    private static final int[][] BODY_TEX = new int[][]{
            {0, 0}, {0, 4}, {0, 9}, {0, 16}, {0, 22}, {11, 0}, {13, 4}
    };
    private static final float[] BODY_Z = new float[7];

    static {
        float z = -3.5F;
        for (int i = 0; i < BODY_SIZES.length; i++) {
            BODY_Z[i] = z;
            if (i < BODY_SIZES.length - 1) {
                z += (BODY_SIZES[i][2] + BODY_SIZES[i + 1][2]) * 0.5F;
            }
        }
    }

    private final ModelPart[] body = new ModelPart[7];
    private final ModelPart[] fins = new ModelPart[3];

    public ParasyticLouseModel(ModelPart root) {
        super(root);
        for (int i = 0; i < body.length; i++) {
            body[i] = root.getChild("body_" + i);
        }
        fins[0] = root.getChild("fin_0");
        fins[1] = root.getChild("fin_1");
        fins[2] = root.getChild("fin_2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        for (int i = 0; i < BODY_SIZES.length; i++) {
            int[] size = BODY_SIZES[i];
            int[] tex = BODY_TEX[i];
            root.addOrReplaceChild("body_" + i,
                    CubeListBuilder.create()
                            .texOffs(tex[0], tex[1])
                            .addBox(size[0] * -0.5F, 0.0F, size[2] * -0.5F, size[0], size[1], size[2]),
                    PartPose.offset(0.0F, 24.0F - size[1], BODY_Z[i]));
        }

        root.addOrReplaceChild("fin_0",
                CubeListBuilder.create().texOffs(20, 0)
                        .addBox(-5.0F, 0.0F, BODY_SIZES[2][2] * -0.5F, 10.0F, 8.0F, BODY_SIZES[2][2]),
                PartPose.offset(0.0F, 16.0F, BODY_Z[2]));
        root.addOrReplaceChild("fin_1",
                CubeListBuilder.create().texOffs(20, 11)
                        .addBox(-3.0F, 0.0F, BODY_SIZES[4][2] * -0.5F, 6.0F, 4.0F, BODY_SIZES[4][2]),
                PartPose.offset(0.0F, 20.0F, BODY_Z[4]));
        root.addOrReplaceChild("fin_2",
                CubeListBuilder.create().texOffs(20, 18)
                        .addBox(-3.0F, 0.0F, BODY_SIZES[4][2] * -0.5F, 6.0F, 5.0F, BODY_SIZES[1][2]),
                PartPose.offset(0.0F, 19.0F, BODY_Z[1]));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float age = state.ageInTicks;
        for (int i = 0; i < body.length; i++) {
            float yRot = Mth.cos(age * 0.9F + i * 0.15F * Mth.PI) * Mth.PI * 0.05F * (1 + Math.abs(i - 2));
            float x = Mth.sin(age * 0.9F + i * 0.15F * Mth.PI) * Mth.PI * 0.2F * Math.abs(i - 2);
            body[i].yRot = yRot;
            body[i].x = x;
        }

        fins[0].yRot = body[2].yRot;
        fins[1].yRot = body[4].yRot;
        fins[1].x = body[4].x;
        fins[2].yRot = body[1].yRot;
        fins[2].x = body[1].x;
    }
}
