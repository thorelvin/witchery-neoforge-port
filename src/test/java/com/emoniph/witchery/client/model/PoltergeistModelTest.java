package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class PoltergeistModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceEightCubeGraphAndAnimationAnchorsArePreserved() {
        ModelPart root = PoltergeistModel.createBodyLayer().bakeRoot();
        assertEquals(9, root.getAllParts().size());
        assertEquals(8, cubes(root).size());
        assertPose(root.getChild("head"), 0, 0, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -5, 2, 0, 0, 0, 0);
        assertPose(root.getChild("left_arm_2"), 5, 2, 0, 0, 0, 0);
        assertPose(root.getChild("right_leg"), -2, 11, 0, 0, 0, 0);

        PoltergeistModel model = new PoltergeistModel(root);
        PoltergeistModel.PoltergeistRenderState state = new PoltergeistModel.PoltergeistRenderState();
        state.walkAnimationPos = 2.4F;
        state.walkAnimationSpeed = 0.65F;
        state.ageInTicks = 11.0F;
        model.setupAnim(state);

        float primary = Mth.cos(2.4F * 0.6662F + Mth.PI) * 2.0F * 0.65F * 0.5F;
        float armBob = Mth.sin(11.0F * 0.067F) * 0.05F;
        assertEquals(primary + armBob, root.getChild("right_arm").xRot, EPSILON);
        assertEquals(primary * 0.5F - armBob, root.getChild("right_arm_2").xRot, EPSILON);
        assertEquals(12.0F, root.getChild("right_leg").y, EPSILON);
        assertEquals(0.1F, root.getChild("right_leg").z, EPSILON);
    }

    @Test
    void attackTimerOverridesAllFourArmPitchesAndPrimaryRollsLikeSource() {
        ModelPart root = PoltergeistModel.createBodyLayer().bakeRoot();
        PoltergeistModel model = new PoltergeistModel(root);
        PoltergeistModel.PoltergeistRenderState state = new PoltergeistModel.PoltergeistRenderState();
        state.attackTimer = 12;
        state.yRot = 3.5F;
        model.setupAnim(state);
        float triangle = PoltergeistModel.sourceTriangle(12.0F - 3.5F, 15.0F);
        float pitch = -1.5F + 0.8F * triangle;
        assertEquals(pitch, root.getChild("right_arm").xRot, EPSILON);
        assertEquals(pitch, root.getChild("right_arm_2").xRot, EPSILON);
        assertEquals(pitch, root.getChild("left_arm").xRot, EPSILON);
        assertEquals(pitch, root.getChild("left_arm_2").xRot, EPSILON);
        assertEquals(-(-1.5F + 1.5F * triangle), root.getChild("right_arm").zRot, EPSILON);
        assertEquals(-1.5F + 1.5F * triangle, root.getChild("left_arm").zRot, EPSILON);
    }

    private static void assertPose(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }
}
