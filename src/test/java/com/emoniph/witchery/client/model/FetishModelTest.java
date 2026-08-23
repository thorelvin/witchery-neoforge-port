package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FetishModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphAndEveryCubeCoordinateArePreserved() {
        ModelPart root = FetishModel.createBodyLayer().bakeRoot();

        assertEquals(10, root.getAllParts().size());
        assertPart(root, "pole_vertical", 0, 9, 0, -1, 0, -1, 1, 15, 1);
        assertPart(root, "pole_horizontal", 0, 13, 0, -8, 0, -0.5F, 8, 1, 0.5F);
        assertPart(root, "head", 0, 12, 0, -2, -4, -2, 2, 1, 2);
        assertPart(root, "head_inner", 0, 12, 0, -2, -4, -1.9F, 2, 1, -1.9F);
        assertPart(root, "body", 0, 12.5F, 0, -3, 0, -1.5F, 3, 9, 1.5F);
        assertPart(root, "arm_left", 3, 13, 0, 0, -0.5F, -1.5F, 3, 3.5F, 1.5F);
        assertPart(root, "arm_left_inner", 3, 13, 0, 2.9F, -0.5F, -1.5F, 2.9F, 3.5F, 1.5F);
        assertPart(root, "arm_right", -3, 13, 0, -3, -0.5F, -1.5F, 0, 3.5F, 1.5F);
        assertPart(root, "arm_right_inner", -3, 13, 0, -2.9F, -0.5F, -1.5F, -2.9F, 3.5F, 1.5F);
        assertEquals(9, cubes(root).size());
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(FetishModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPart(ModelPart root, String name,
                                   float pivotX, float pivotY, float pivotZ,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(pivotX, part.x, EPSILON, name + " pivotX");
        assertEquals(pivotY, part.y, EPSILON, name + " pivotY");
        assertEquals(pivotZ, part.z, EPSILON, name + " pivotZ");
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
