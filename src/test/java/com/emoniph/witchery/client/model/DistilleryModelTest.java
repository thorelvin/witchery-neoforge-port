package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DistilleryModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphGeometryPivotsAndRotationsArePreserved() {
        ModelPart root = DistilleryModel.createBodyLayer().bakeRoot();

        assertEquals(14, root.getAllParts().size());
        assertPart(root, "still_base", -5, 18, -2, 0, 0, 0, 10, 6, 10);
        assertPart(root, "still_middle", -3, 14, 0, 0, 0, 0, 6, 4, 6);
        assertPart(root, "still_top", -2, 11, 1, 0, 0, 0, 4, 3, 4);
        assertPart(root, "still_bend", -1, 9, -1, 0, 0, 0, 2, 2, 4);
        assertPart(root, "still_tube", 0, 10, 0, -0.5F, -0.5F, 0, 0.5F, 0.5F, 8);
        assertEquals(-2.341978F, root.getChild("still_tube").xRot, EPSILON);
        assertPart(root, "frame_top", -8, 15, -6, 0, 0, 0, 16, 1, 1);
        assertPart(root, "frame_left", -8, 16, -6, 0, 0, 0, 1, 7, 1);
        assertPart(root, "frame_right", 7, 16, -6, 0, 0, 0, 1, 7, 1);
        assertPart(root, "frame_base", -8, 23, -8, 0, 0, 0, 16, 1, 5);

        assertBottle(root.getChild("bottle1"), -7, 16, -7, 0);
        assertBottle(root.getChild("bottle2"), -3.3F, 16, -7, 0.0174533F);
        assertBottle(root.getChild("bottle3"), 0.4F, 16, -7, 0);
        assertBottle(root.getChild("bottle4"), 4, 16, -7, 0);
        assertEquals(21, cubes(root).size());
    }

    @Test
    void sourceMirrorTimingIsPreserved() throws ReflectiveOperationException {
        ModelPart root = DistilleryModel.createBodyLayer().bakeRoot();
        for (String name : List.of("still_base", "still_middle", "still_top", "still_bend", "still_tube",
                "frame_top", "frame_left", "frame_right", "frame_base")) {
            assertEquals(-1.0F, westFaceNormal(cubes(root.getChild(name)).getFirst()).x(), EPSILON, name);
        }
        for (String name : List.of("bottle1", "bottle2", "bottle3", "bottle4")) {
            for (ModelPart.Cube cube : cubes(root.getChild(name))) {
                assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON, name);
            }
        }
    }

    @Test
    void jarVisibilityMatchesTheSourceSlotCountCap() {
        ModelPart root = DistilleryModel.createBodyLayer().bakeRoot();
        DistilleryModel model = new DistilleryModel(root);

        model.setJarCount(0);
        assertFalse(root.getChild("bottle1").visible);
        assertFalse(root.getChild("bottle4").visible);
        model.setJarCount(2);
        assertTrue(root.getChild("bottle1").visible);
        assertTrue(root.getChild("bottle2").visible);
        assertFalse(root.getChild("bottle3").visible);
        model.setJarCount(9);
        assertTrue(root.getChild("bottle4").visible);
    }

    private static void assertBottle(ModelPart bottle, float x, float y, float z, float xRot) {
        assertPose(bottle, x, y, z);
        assertEquals(xRot, bottle.xRot, EPSILON);
        List<ModelPart.Cube> cubes = cubes(bottle);
        assertEquals(3, cubes.size());
        assertCube(cubes.get(0), 0, 2, 0, 3, 5, 3, "body");
        assertCube(cubes.get(1), 1, 1, 1, 2, 2, 2, "neck");
        assertCube(cubes.get(2), 0.5F, 0, 0.5F, 2.5F, 1, 2.5F, "top");
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
