package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SpectralFamiliarModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void exactSourcePigGraphUsesSevenUnmirroredCubes() throws ReflectiveOperationException {
        ModelPart root = SpectralFamiliarModel.createBodyLayer().bakeRoot();
        assertEquals(7, root.getAllParts().size());
        assertEquals(7, cubes(root).size());
        assertPose(root.getChild("head"), 0, 12, -6, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 11, 2, Mth.HALF_PI, 0, 0);
        assertPose(root.getChild("right_hind_leg"), -3, 18, 7, 0, 0, 0);
        assertPose(root.getChild("left_hind_leg"), 3, 18, 7, 0, 0, 0);
        assertPose(root.getChild("right_front_leg"), -3, 18, -5, 0, 0, 0);
        assertPose(root.getChild("left_front_leg"), 3, 18, -5, 0, 0, 0);

        List<ModelPart.Cube> headCubes = directCubes(root.getChild("head"));
        assertEquals(2, headCubes.size());
        assertCube(headCubes.getFirst(), -4, -4, -8, 4, 4, 0);
        assertCube(headCubes.getLast(), -2, 0, -9, 2, 3, -8);
        for (ModelPart.Cube cube : cubes(root)) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void walkingAndSittingUseTheExactFamiliarPoseRules() {
        ModelPart root = SpectralFamiliarModel.createBodyLayer().bakeRoot();
        SpectralFamiliarModel model = new SpectralFamiliarModel(root);
        SpectralFamiliarModel.State state = new SpectralFamiliarModel.State();
        state.walkAnimationPos = 1.7F;
        state.walkAnimationSpeed = 0.55F;
        state.xRot = 12.0F;
        state.yRot = -20.0F;

        model.setupAnim(state);
        float walk = 1.7F * 0.6662F;
        assertEquals(Mth.cos(walk + Mth.PI) * 0.55F,
                model.sourceRightHindLeg().xRot, EPSILON);
        assertEquals(Mth.cos(walk) * 0.55F,
                model.sourceLeftHindLeg().xRot, EPSILON);
        assertEquals(Mth.cos(walk) * 0.55F,
                model.sourceRightFrontLeg().xRot, EPSILON);
        assertEquals(Mth.cos(walk + Mth.PI) * 0.55F,
                model.sourceLeftFrontLeg().xRot, EPSILON);
        assertEquals(12.0F * Mth.DEG_TO_RAD, model.sourceHead().xRot, EPSILON);
        assertEquals(-20.0F * Mth.DEG_TO_RAD, model.sourceHead().yRot, EPSILON);

        state.sitting = true;
        model.setupAnim(state);
        assertPose(model.sourceBody(), 0, 14.5F, 2, Mth.PI / 4.0F, 0, 0);
        assertPose(model.sourceRightHindLeg(), -3, 15.8F, -7,
                -Mth.PI / 20.0F, 0, 0);
        assertPose(model.sourceLeftHindLeg(), 3, 15.8F, -7,
                -Mth.PI / 20.0F, 0, 0);
        assertPose(model.sourceRightFrontLeg(), -3, 21, 1,
                -Mth.HALF_PI, 0, 0);
        assertPose(model.sourceLeftFrontLeg(), 3, 21, 1,
                -Mth.HALF_PI, 0, 0);
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

    private static List<ModelPart.Cube> directCubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (path.isEmpty()) {
                cubes.add(cube);
            }
        });
        return cubes;
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static Vector3f westFaceNormal(ModelPart.Cube cube) throws ReflectiveOperationException {
        Object[] polygons = polygons(cube);
        for (Field field : polygons[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons[2]);
            if (value instanceof Vector3f normal) {
                return normal;
            }
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }

    private static Object[] polygons(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        return (Object[]) polygonsField.get(cube);
    }
}
