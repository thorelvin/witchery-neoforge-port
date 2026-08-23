package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ChaliceModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceChaliceHierarchyPivotsAndCubeBoundsArePreserved() {
        ModelPart root = ChaliceModel.createBodyLayer().bakeRoot();
        ModelPart chalice = root.getChild("chalice");
        ModelPart liquid = root.getChild("liquid");

        assertEquals(3, root.getAllParts().size());
        assertPose(chalice, -1, 23, -1);
        assertPose(liquid, -2, 19, -2);

        List<ModelPart.Cube> cupCubes = cubesAtPath(chalice, "");
        assertEquals(7, cupCubes.size());
        assertCube(cupCubes.get(0), 4, -6, -1, 4, -2, 4);
        assertCube(cupCubes.get(1), -1, -6, -1, -1, -2, 4);
        assertCube(cupCubes.get(2), -1, -6, 4, 4, -2, 4);
        assertCube(cupCubes.get(3), -1, -6, -1, 4, -2, -1);
        assertCube(cupCubes.get(4), -1, -2, -1, 4, -2, 4);
        assertCube(cupCubes.get(5), 1, -2, 1, 2, 0, 2);
        assertCube(cupCubes.get(6), 0, 0, 0, 3, 1, 3);

        List<ModelPart.Cube> liquidCubes = cubesAtPath(liquid, "");
        assertEquals(1, liquidCubes.size());
        assertCube(liquidCubes.getFirst(), 0, 0, 0, 5, 0, 5);
    }

    @Test
    void sourceCupMirrorFlagIsCapturedByEveryCupCube() throws ReflectiveOperationException {
        ModelPart chalice = ChaliceModel.createBodyLayer().bakeRoot().getChild("chalice");
        for (ModelPart.Cube cube : cubesAtPath(chalice, "")) {
            assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON);
        }
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

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
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
