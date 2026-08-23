package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DemonHeartModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphGeometryPivotsAndRotationsArePreserved() {
        ModelPart root = DemonHeartModel.createBodyLayer().bakeRoot();

        assertEquals(11, root.getAllParts().size());
        assertPart(root, "shape1", -3, 14, 0, 0, 0, 0, 0, 0, 0, 5, 8, 4);
        assertPart(root, "shape2", -4, 15, -1, 0, 0, 0, 0, 0, 0, 3, 8, 6);
        assertPart(root, "shape3", -5, 16, 0, 0, 0, 0, 0, 0, 0, 1, 6, 4);
        assertPart(root, "shape4", -3, 13, 1, 0, 0, 0, 0, 0, 0, 2, 1, 2);
        assertPart(root, "big_tube1", -2, 15, -1, 0, 0.3717861F, 0.2230717F, 0, 0, 0, 3, 2, 2);
        assertPart(root, "tube1", -3, 14, 1, 0.4089647F, 0.6320364F, 0, 0, -3, 1, 1, 0, 2);
        assertPart(root, "tube2", -2, 14, 1, -0.2974289F, -0.2230717F, -0.3346075F, 0, -3, 1, 1, 0, 2);
        assertPart(root, "tube3", 1, 13, -0.8F, -0.0743572F, 0.1487144F, -0.2602503F, 0, -3, 1, 1, 0, 2);
        assertPart(root, "tube4", 0, 15, 0, 0.2602503F, 0, 0.4089647F, 0, -3, 1, 1, 0, 2);
        assertPart(root, "tube5", 0, 14, 1, -0.2602503F, 0, 0, 0, -3, 1, 1, 0, 2);
        assertEquals(10, cubes(root).size());
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(DemonHeartModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPart(ModelPart root, String name,
                                   float pivotX, float pivotY, float pivotZ,
                                   float xRot, float yRot, float zRot,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(pivotX, part.x, EPSILON, name + " pivotX");
        assertEquals(pivotY, part.y, EPSILON, name + " pivotY");
        assertEquals(pivotZ, part.z, EPSILON, name + " pivotZ");
        assertEquals(xRot, part.xRot, EPSILON, name + " xRot");
        assertEquals(yRot, part.yRot, EPSILON, name + " yRot");
        assertEquals(zRot, part.zRot, EPSILON, name + " zRot");
        ModelPart.Cube cube = cubes(part).getFirst();
        assertEquals(minX, cube.minX, EPSILON, name + " minX");
        assertEquals(minY, cube.minY, EPSILON, name + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, name + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, name + " maxZ");
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
