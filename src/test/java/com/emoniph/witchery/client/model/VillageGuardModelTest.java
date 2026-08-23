package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class VillageGuardModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sharedSourceGraphKeepsTheTallHeadStandardHatNoseAndGuardRobe() {
        ModelPart root = VillageGuardModel.createBodyLayer().bakeRoot();

        assertEquals(11, root.getAllParts().size());
        assertEquals(9, cubes(root).size());
        assertPose(root.getChild("head"), 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("head").getChild("nose"), 0.0F, -2.0F, 0.0F);
        assertPose(root.getChild("head").getChild("hat"), 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("source_hat"), 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("robe"), 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("right_leg"), -1.9F, 12.0F, 0.0F);
        assertPose(root.getChild("left_leg"), 1.9F, 12.0F, 0.0F);

        assertCube(cubes(root.getChild("head")).get(0),
                -4.0F, -10.0F, -4.0F, 4.0F, 0.0F, 4.0F);
        assertCube(cubes(root.getChild("head").getChild("nose")).get(0),
                -1.0F, -1.0F, -6.0F, 1.0F, 3.0F, -4.0F);
        assertCube(cubes(root.getChild("source_hat")).get(0),
                -4.0F, -8.0F, -4.0F, 4.0F, 0.0F, 4.0F);
        assertCube(cubes(root.getChild("robe")).get(0),
                -4.0F, 0.0F, -3.0F, 4.0F, 18.0F, 3.0F);
    }

    @Test
    void guardUsesHalfLegGaitAndRobeWhileVampireUsesFullGaitWithoutRobe() {
        LegacyBipedModel.State state = new LegacyBipedModel.State();
        state.walkAnimationPos = 1.25F;
        state.walkAnimationSpeed = 0.6F;
        state.xRot = 20.0F;
        state.yRot = -35.0F;

        ModelPart guardRoot = VillageGuardModel.createBodyLayer().bakeRoot();
        new VillageGuardModel<>(guardRoot, true).setupAnim(state);
        float rightLeg = Mth.cos(1.25F * 0.6662F) * 1.4F * 0.6F;
        float leftLeg = Mth.cos(1.25F * 0.6662F + Mth.PI) * 1.4F * 0.6F;
        assertEquals(rightLeg * 0.5F, guardRoot.getChild("right_leg").xRot, EPSILON);
        assertEquals(leftLeg * 0.5F, guardRoot.getChild("left_leg").xRot, EPSILON);
        assertEquals(0.3F, guardRoot.getChild("source_hat").y, EPSILON);
        assertEquals(20.0F * Mth.DEG_TO_RAD, guardRoot.getChild("source_hat").xRot, EPSILON);
        assertEquals(-35.0F * Mth.DEG_TO_RAD, guardRoot.getChild("source_hat").yRot, EPSILON);
        assertFalse(guardRoot.getChild("head").getChild("hat").visible);
        assertTrue(guardRoot.getChild("robe").visible);

        ModelPart vampireRoot = VillageGuardModel.createBodyLayer().bakeRoot();
        new VillageGuardModel<>(vampireRoot, false).setupAnim(state);
        assertEquals(rightLeg, vampireRoot.getChild("right_leg").xRot, EPSILON);
        assertEquals(leftLeg, vampireRoot.getChild("left_leg").xRot, EPSILON);
        assertEquals(0.3F, vampireRoot.getChild("source_hat").y, EPSILON);
        assertFalse(vampireRoot.getChild("head").getChild("hat").visible);
        assertFalse(vampireRoot.getChild("robe").visible);
    }

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static void assertCube(ModelPart.Cube cube,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
    }
}
