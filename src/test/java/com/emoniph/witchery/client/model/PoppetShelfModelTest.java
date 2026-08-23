package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PoppetShelfModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceTableGeometryAndPivotArePreserved() {
        ModelPart root = PoppetShelfModel.createBodyLayer().bakeRoot();
        ModelPart table = root.getChild("table");

        assertEquals(2, root.getAllParts().size());
        assertEquals(0.0F, table.x, EPSILON);
        assertEquals(0.0F, table.y, EPSILON);
        assertEquals(0.0F, table.z, EPSILON);
        ModelPart.Cube cube = cubes(table).getFirst();
        assertEquals(0.0F, cube.minX, EPSILON);
        assertEquals(0.0F, cube.minY, EPSILON);
        assertEquals(0.0F, cube.minZ, EPSILON);
        assertEquals(16.0F, cube.maxX, EPSILON);
        assertEquals(8.0F, cube.maxY, EPSILON);
        assertEquals(16.0F, cube.maxZ, EPSILON);
    }

    @Test
    void sourceMirrorAssignmentAfterAddBoxDoesNotMirrorTheTable() throws ReflectiveOperationException {
        ModelPart table = PoppetShelfModel.createBodyLayer().bakeRoot().getChild("table");
        assertEquals(-1.0F, westFaceNormal(cubes(table).getFirst()).x(), EPSILON);
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
