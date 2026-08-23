package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

class HellhoundModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceGeometryPivotsAndRootLayoutArePreserved() {
        ModelPart root = HellhoundModel.createBodyLayer().bakeRoot();

        assertEquals(9, root.getAllParts().size());
        assertPose(root.getChild("head"), -1.0F, 10.6F, -7.5F);
        assertPose(root.getChild("body"), 0.0F, 12.5F, 2.0F);
        assertPose(root.getChild("mane"), -1.0F, 14.0F, -3.0F);
        assertPose(root.getChild("tail"), -1.0F, 12.0F, 8.0F);
        assertPose(root.getChild("leg_back_left"), -2.5F, 16.0F, 7.0F);
        assertPose(root.getChild("leg_back_right"), 0.5F, 16.0F, 7.0F);
        assertPose(root.getChild("leg_front_left"), -2.5F, 16.0F, -4.0F);
        assertPose(root.getChild("leg_front_right"), 0.5F, 16.0F, -4.0F);

        List<ModelPart.Cube> cubes = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        assertEquals(11, cubes.size());

        List<ModelPart.Cube> headCubes = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            if ("/head".equals(path)) {
                headCubes.add(cube);
            }
        });
        assertEquals(4, headCubes.size());
        assertCube(headCubes.get(0), -3.0F, -3.0F, -2.0F, 3.0F, 3.0F, 2.0F);
        assertCube(headCubes.get(1), -3.0F, -5.0F, 1.0F, -1.0F, -3.0F, 2.0F);
        assertCube(headCubes.get(2), 1.0F, -5.0F, 1.0F, 3.0F, -3.0F, 2.0F);
        assertCube(headCubes.get(3), -1.5F, 0.0F, -5.0F, 1.5F, 3.0F, -1.0F);
    }

    @Test
    void livePoseMatchesSourceValuesAndRenderWithRotationOrder() {
        ModelPart root = HellhoundModel.createBodyLayer().bakeRoot();
        HellhoundModel model = new HellhoundModel(root);
        HellhoundModel.HellhoundRenderState state = new HellhoundModel.HellhoundRenderState();
        state.walkAnimationPos = 1.25F;
        state.walkAnimationSpeed = 0.6F;
        state.xRot = 20.0F;
        state.yRot = -35.0F;
        state.headRoll = 0.23F;
        state.maneRoll = 0.17F;
        state.bodyRoll = -0.11F;
        state.tailRoll = 0.31F;

        model.setupAnim(state);

        float headX = 20.0F * Mth.DEG_TO_RAD + 0.15F;
        assertLegacyYxzRotation(root.getChild("head"), headX, -35.0F * Mth.DEG_TO_RAD, 0.23F);
        assertEquals(1.1838568F, root.getChild("body").xRot, EPSILON);
        assertEquals(-0.11F, root.getChild("body").zRot, EPSILON);
        assertEquals(Mth.HALF_PI, root.getChild("mane").xRot, EPSILON);
        assertEquals(0.17F, root.getChild("mane").zRot, EPSILON);

        ModelPart tail = root.getChild("tail");
        assertPose(tail, -1.0F, 13.0F, 8.0F);
        assertLegacyYxzRotation(tail, 0.59184116F + 1.5F * 0.6F - 0.1F, 0.0F, 0.31F);

        float phase = Mth.cos(1.25F * 0.6662F) * 1.4F * 0.6F;
        float antiPhase = Mth.cos(1.25F * 0.6662F + Mth.PI) * 1.4F * 0.6F;
        assertEquals(phase, root.getChild("leg_back_left").xRot, EPSILON);
        assertEquals(phase, root.getChild("leg_front_right").xRot, EPSILON);
        assertEquals(antiPhase, root.getChild("leg_back_right").xRot, EPSILON);
        assertEquals(antiPhase, root.getChild("leg_front_left").xRot, EPSILON);
    }

    @Test
    void conversionUsesTheSourceHeadAndTailPitch() {
        ModelPart root = HellhoundModel.createBodyLayer().bakeRoot();
        HellhoundModel model = new HellhoundModel(root);
        HellhoundModel.HellhoundRenderState state = new HellhoundModel.HellhoundRenderState();
        state.converting = true;

        model.setupAnim(state);

        assertLegacyYxzRotation(root.getChild("head"), 0.5F, 0.0F, 0.0F);
        assertLegacyYxzRotation(root.getChild("tail"), 2.0F, 0.0F, 0.0F);
    }

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
    }

    private static void assertLegacyYxzRotation(ModelPart part, float xRot, float yRot, float zRot) {
        Quaternionf expected = new Quaternionf().rotateY(yRot).rotateX(xRot).rotateZ(zRot);
        Quaternionf actual = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
        assertEquals(1.0F, Math.abs(expected.dot(actual)), EPSILON);
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
