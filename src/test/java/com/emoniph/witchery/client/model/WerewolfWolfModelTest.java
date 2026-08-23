package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class WerewolfWolfModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void exactLegacyWolfGeometryAndAsymmetricPivotsArePreserved() {
        ModelPart root = WerewolfWolfModel.createBodyLayer().bakeRoot();
        assertEquals(9, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertPose(root.getChild("head"), -1.0F, 13.5F, -7.0F, 0.0F);
        assertPose(root.getChild("body"), 0.0F, 14.0F, 2.0F, 0.0F);
        assertPose(root.getChild("upper_body"), -1.0F, 14.0F, 2.0F, 0.0F);
        assertPose(root.getChild("right_hind_leg"), -2.5F, 16.0F, 7.0F, 0.0F);
        assertPose(root.getChild("left_hind_leg"), 0.5F, 16.0F, 7.0F, 0.0F);
        assertPose(root.getChild("right_front_leg"), -2.5F, 16.0F, -4.0F, 0.0F);
        assertPose(root.getChild("left_front_leg"), 0.5F, 16.0F, -4.0F, 0.0F);
        assertPose(root.getChild("tail"), -1.0F, 12.0F, 8.0F, 0.0F);
    }

    @Test
    void exactStandingGaitLookTailAndShakePoseArePreserved() {
        ModelPart root = WerewolfWolfModel.createBodyLayer().bakeRoot();
        WerewolfWolfModel model = new WerewolfWolfModel(root);
        WerewolfWolfModel.WolfRenderState state = new WerewolfWolfModel.WolfRenderState();
        state.walkAnimationPos = 3.0F;
        state.walkAnimationSpeed = 0.7F;
        state.ageInTicks = 12.25F;
        state.xRot = 15.0F;
        state.yRot = -20.0F;
        state.interestedAngle = 0.1F;
        state.headShakeAngle = 0.2F;
        state.upperBodyShakeAngle = 0.3F;
        state.bodyShakeAngle = 0.4F;
        state.tailShakeAngle = 0.5F;
        model.setupAnim(state);

        float right = Mth.cos(3.0F * 0.6662F) * 1.4F * 0.7F;
        float left = Mth.cos(3.0F * 0.6662F + Mth.PI) * 1.4F * 0.7F;
        assertEquals(right, root.getChild("right_hind_leg").xRot, EPSILON);
        assertEquals(left, root.getChild("left_hind_leg").xRot, EPSILON);
        assertEquals(left, root.getChild("right_front_leg").xRot, EPSILON);
        assertEquals(right, root.getChild("left_front_leg").xRot, EPSILON);
        assertEquals(Mth.HALF_PI, root.getChild("body").xRot, EPSILON);
        assertPose(root.getChild("upper_body"), -1.0F, 14.0F, -3.0F, Mth.HALF_PI);
        assertEquals(12.25F, root.getChild("tail").xRot, EPSILON);
        assertEquals(right, root.getChild("tail").yRot, EPSILON);
        assertEquals(0.5F, root.getChild("tail").zRot, EPSILON);
        assertEquals(0.3F, root.getChild("head").zRot, EPSILON);
        assertEquals(0.3F, root.getChild("upper_body").zRot, EPSILON);
        assertEquals(0.4F, root.getChild("body").zRot, EPSILON);
    }

    @Test
    void exactLegacySittingPoseAndAngryTailArePreserved() {
        ModelPart root = WerewolfWolfModel.createBodyLayer().bakeRoot();
        WerewolfWolfModel model = new WerewolfWolfModel(root);
        WerewolfWolfModel.WolfRenderState state = new WerewolfWolfModel.WolfRenderState();
        state.sitting = true;
        state.angry = true;
        model.setupAnim(state);

        assertPose(root.getChild("upper_body"), -1.0F, 16.0F, -3.0F, 1.2566371F);
        assertPose(root.getChild("body"), 0.0F, 18.0F, 0.0F, 0.7853982F);
        assertPose(root.getChild("tail"), -1.0F, 21.0F, 6.0F, 0.0F);
        assertPose(root.getChild("right_hind_leg"), -2.5F, 22.0F, 2.0F, 4.712389F);
        assertPose(root.getChild("left_hind_leg"), 0.5F, 22.0F, 2.0F, 4.712389F);
        assertPose(root.getChild("right_front_leg"), -2.49F, 17.0F, -4.0F, 5.811947F);
        assertPose(root.getChild("left_front_leg"), 0.51F, 17.0F, -4.0F, 5.811947F);
        assertEquals(0.0F, root.getChild("tail").yRot, EPSILON);
    }

    private static void assertPose(ModelPart part, float x, float y, float z, float xRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }
}
