package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MandrakeStatueModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixPartGraphAndEightCubesArePreserved() {
        ModelPart root = MandrakeStatueModel.createBodyLayer().bakeRoot();
        assertEquals(7, root.getAllParts().size());
        assertEquals(8, cubes(root).size());

        assertPose(root, "head", 0, 16, 0, 0, 0, 0, 2);
        assertCube(cubes(root.getChild("head")).get(0), -2, -4, -2, 2, 0, 2);
        assertCube(cubes(root.getChild("head")).get(1), -4, -12, 0, 4, -4, 0);
        assertPose(root, "body", 0, 16, 0, 0, 0, 0, 2);
        assertCube(cubes(root.getChild("body")).get(0), -2.5F, 0, -2.5F, 2.5F, 2, 2.5F);
        assertCube(cubes(root.getChild("body")).get(1), -3.5F, 2, -3.5F, 3.5F, 5, 3.5F);
        assertPose(root, "right_arm", -2, 17, 0, 0, 0, 1.047198F, 1);
        assertPose(root, "left_arm", 2, 17, 0, 0, 0, -1.047198F, 1);
        assertPose(root, "right_leg", -1, 21, 0, 0, 0, 0, 1);
        assertPose(root, "left_leg", 1, 21, 0, 0, 0, 0, 1);
    }

    @Test
    void onlyHeadAndBodyCubesUseTheSourceMirrorWinding() throws ReflectiveOperationException {
        ModelPart root = MandrakeStatueModel.createBodyLayer().bakeRoot();
        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(4, mirrored);
    }

    private static void assertPose(ModelPart root, String name, float x, float y, float z,
                                   float xRot, float yRot, float zRot, int cubeCount) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        assertEquals(xRot, part.xRot, EPSILON, name + " xRot");
        assertEquals(yRot, part.yRot, EPSILON, name + " yRot");
        assertEquals(zRot, part.zRot, EPSILON, name + " zRot");
        assertEquals(cubeCount, cubes(part).size(), name + " cube count");
    }

    private static void assertCube(ModelPart.Cube cube, float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static Vector3f westFaceNormal(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        Object[] polygons = (Object[]) polygonsField.get(cube);
        for (Field field : polygons[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons[2]);
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
