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

class GhostModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSevenCubeGraphAndFixedBodyAnchorArePreserved() {
        ModelPart root = GhostModel.createBodyLayer().bakeRoot();
        assertEquals(8, root.getAllParts().size());
        assertEquals(7, cubes(root).size());
        assertPose(root.getChild("head"), 0, 0, 0, 0, 0, 0);
        assertPose(root.getChild("head").getChild("mouth"), -2, -4, -4.02F, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -5, 2, 0, -1.396263F, 0, 0);
        assertPose(root.getChild("robe_upper"), 0, 10, 0, 0, 0, 0);
        assertPose(root.getChild("robe_lower"), 0, 16, 0, 0, 0, 0);

        GhostModel model = new GhostModel(root, false);
        GhostModel.GhostRenderState state = new GhostModel.GhostRenderState();
        state.ageInTicks = 23.0F;
        model.setupAnim(state);
        assertEquals(0.0F, root.getChild("body").y, EPSILON);
    }

    @Test
    void bansheeAndSpectreArmMouthAndLookStatesMatchSource() {
        ModelPart root = GhostModel.createBodyLayer().bakeRoot();
        GhostModel banshee = new GhostModel(root, false);
        GhostModel.GhostRenderState state = new GhostModel.GhostRenderState();
        state.ageInTicks = 13.0F;
        state.yRot = 17.0F;
        state.xRot = -8.0F;
        state.screaming = true;
        banshee.setupAnim(state);
        float bob = Mth.sin(13.0F * 0.067F) * 0.05F;
        assertTrue(root.getChild("head").getChild("mouth").visible);
        assertEquals(-0.2F + bob, root.getChild("right_arm").xRot, EPSILON);
        assertEquals(-0.2F - bob, root.getChild("left_arm").xRot, EPSILON);
        assertEquals(1.0F, root.getChild("right_arm").zRot, EPSILON);
        assertEquals(-1.0F, root.getChild("left_arm").zRot, EPSILON);
        assertEquals(17.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);

        ModelPart spectreRoot = GhostModel.createBodyLayer().bakeRoot();
        GhostModel spectre = new GhostModel(spectreRoot, true);
        state.screaming = false;
        spectre.setupAnim(state);
        assertFalse(spectreRoot.getChild("head").getChild("mouth").visible);
        assertEquals(-1.5F + bob, spectreRoot.getChild("right_arm").xRot, EPSILON);
        assertEquals(-1.5F - bob, spectreRoot.getChild("left_arm").xRot, EPSILON);
        assertEquals(0.0F, spectreRoot.getChild("right_arm").zRot, EPSILON);
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
