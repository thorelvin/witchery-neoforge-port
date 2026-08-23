package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WitchesOvenModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphGeometryAndPivotsArePreserved() {
        ModelPart root = WitchesOvenModel.createBodyLayer().bakeRoot();

        assertEquals(10, root.getAllParts().size());
        assertPart(root, "body", -6, 14, -6, 0, 1, 0, 12, 9, 12);
        assertPart(root, "lid_bottom", -7, 14, -7, 0, 0, 0, 14, 1, 14);
        assertPart(root, "lid_top", -5, 13, -5, 0, 0, 0, 10, 1, 10);
        assertPart(root, "chimney", -2, 8, 3, 0, 0, 0, 4, 13, 4);
        assertPart(root, "chimney_top", -2, 8, 7, 0, 0, 0, 4, 4, 1);
        assertRightLeg(root.getChild("leg_back_right"), -5, 21, -7);
        assertRightLeg(root.getChild("leg_front_right"), -5, 21, 6);
        assertLeftLeg(root.getChild("leg_back_left"), 5, 21, -7);
        assertLeftLeg(root.getChild("leg_front_left"), 5, 21, 6);
        assertEquals(13, cubes(root).size());
    }

    @Test
    void sourceMirrorTimingIsPreserved() throws ReflectiveOperationException {
        ModelPart root = WitchesOvenModel.createBodyLayer().bakeRoot();
        for (String name : List.of("body", "lid_bottom", "lid_top", "chimney", "chimney_top")) {
            assertEquals(-1.0F, westFaceNormal(cubes(root.getChild(name)).getFirst()).x(), EPSILON, name);
        }
        for (String name : List.of("leg_back_right", "leg_front_right", "leg_back_left", "leg_front_left")) {
            for (ModelPart.Cube cube : cubes(root.getChild(name))) {
                assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON, name);
            }
        }
    }

    private static void assertRightLeg(ModelPart leg, float x, float y, float z) {
        assertPose(leg, x, y, z);
        List<ModelPart.Cube> cubes = cubes(leg);
        assertEquals(2, cubes.size());
        assertCube(cubes.get(0), -2, 0, 0, 0, 1, 1, "horizontal foot");
        assertCube(cubes.get(1), -3, 0, 0, -2, 3, 1, "vertical post");
    }

    private static void assertLeftLeg(ModelPart leg, float x, float y, float z) {
        assertPose(leg, x, y, z);
        List<ModelPart.Cube> cubes = cubes(leg);
        assertEquals(2, cubes.size());
        assertCube(cubes.get(0), 0, 0, 0, 2, 1, 1, "horizontal foot");
        assertCube(cubes.get(1), 2, 0, 0, 3, 3, 1, "vertical post");
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertPose(part, x, y, z);
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size(), name + " cube count");
        assertCube(cubes.getFirst(), minX, minY, minZ, maxX, maxY, maxZ, name);
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
            if (value instanceof Vector3f normal) {
                return normal;
            }
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
    }

    private static void assertCube(ModelPart.Cube cube,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ, String label) {
        assertEquals(minX, cube.minX, EPSILON, label + " minX");
        assertEquals(minY, cube.minY, EPSILON, label + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, label + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, label + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, label + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, label + " maxZ");
    }
}
