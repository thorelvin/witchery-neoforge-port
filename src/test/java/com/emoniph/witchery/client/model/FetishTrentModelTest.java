package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FetishTrentModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphGeometryPivotsAndRotationsArePreserved() {
        ModelPart root = FetishTrentModel.createBodyLayer().bakeRoot();

        assertEquals(12, root.getAllParts().size());
        assertPart(root, "body", 0, 12, 0, 0, 0, 0, -3, 0, -3, 3, 9, 3);
        assertPart(root, "face", 0, 12, 0, 0, 0, 0, -3, 1, -2.9F, 3, 8, -2.9F);
        assertPart(root, "arm_left", 2, 13, 0, -0.1858931F, 0, -0.7435722F, -1, 0, -1, 1, 5, 1);
        assertPart(root, "arm_right", -2, 13, 0, -0.1858931F, 0, 0.8551081F, -1, 0, -1, 1, 5, 1);
        assertPart(root, "leg_left_upper", 2, 18, 0, -0.1487144F, 0, -0.2602503F, -1, 0, -1, 1, 5, 1);
        assertPart(root, "leg_left_lower", 3, 21, -0.5F, 0.0743572F, 0, -0.1115358F, -0.5F, 0, -0.5F, 0.5F, 3, 0.5F);
        assertPart(root, "leg_right_upper", -2, 18, 0, 0.1858931F, 0, 0.3346075F, -1, 0, -1, 1, 5, 1);
        assertPart(root, "leg_right_lower", -3, 21, 0.5F, 0.1858931F, 0, 0.2230717F, -0.5F, 0, -0.5F, 0.5F, 3, 0.5F);
        assertPart(root, "headdress_1", 0, 13, 1, 0.1115358F, 0, -2.862753F, -1, 0, -1, 1, 5, 1);
        assertPart(root, "headdress_2", -1, 13, 0, 0.3717861F, 0, 2.639681F, -1, 0, -1, 1, 5, 1);
        assertPart(root, "headdress_3", -1, 13, 0, -0.4461433F, 0, 2.862753F, -1, 0, -1, 1, 5, 1);
        assertEquals(11, cubes(root).size());
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(FetishTrentModel.createBodyLayer().bakeRoot())) {
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
