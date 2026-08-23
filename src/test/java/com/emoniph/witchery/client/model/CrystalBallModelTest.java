package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CrystalBallModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphAndEveryCubeCoordinateArePreserved() {
        ModelPart root = CrystalBallModel.createBodyLayer().bakeRoot();

        assertEquals(7, root.getAllParts().size());
        assertPart(root, "base_bottom", -3, 23, -3, 6, 1, 6);
        assertPart(root, "base_middle", -2, 22, -2, 4, 1, 4);
        assertPart(root, "base_top", -1, 21, -1, 2, 1, 2);
        assertPart(root, "globe_inner", -1, 17, -1, 2, 2, 2);
        assertPart(root, "globe_middle", -2, 16, -2, 4, 4, 4);
        assertPart(root, "globe_outer", -3, 15, -3, 6, 6, 6);
        assertEquals(6, cubes(root).size());
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxDoNotMirrorExistingCubes() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(CrystalBallModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float sizeX, float sizeY, float sizeZ) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size(), name + " cube count");
        ModelPart.Cube cube = cubes.getFirst();
        assertEquals(0.0F, cube.minX, EPSILON, name + " minX");
        assertEquals(0.0F, cube.minY, EPSILON, name + " minY");
        assertEquals(0.0F, cube.minZ, EPSILON, name + " minZ");
        assertEquals(sizeX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(sizeY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(sizeZ, cube.maxZ, EPSILON, name + " maxZ");
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
}
