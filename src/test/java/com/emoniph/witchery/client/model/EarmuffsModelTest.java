package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class EarmuffsModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceFiveCubeGeometryAndHeadLocalCoordinatesArePreserved() {
        ModelPart root = EarmuffsModel.createBodyLayer().bakeRoot();
        assertEquals(6, root.getAllParts().size());
        assertEquals(5, cubes(root).size());
        assertPart(root, "band_top", -4, -10, -0.5F, 4, -9, 0.5F);
        assertPart(root, "ear_right", -6, -6, -2, -4, -2, 2);
        assertPart(root, "ear_left", 4, -6, -2, 6, -2, 2);
        assertPart(root, "band_left", 4, -10, -0.5F, 5, -6, 0.5F);
        assertPart(root, "band_right", -5, -10, -0.5F, -4, -6, 0.5F);
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored()
            throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(EarmuffsModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPart(ModelPart root, String name,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(0.0F, part.x, EPSILON);
        assertEquals(0.0F, part.y, EPSILON);
        assertEquals(0.0F, part.z, EPSILON);
        assertCube(cubesAtPath(part, "").getFirst(), minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static List<ModelPart.Cube> cubesAtPath(ModelPart part, String wantedPath) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (wantedPath.equals(path)) {
                cubes.add(cube);
            }
        });
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
