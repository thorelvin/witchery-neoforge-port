package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MirrorModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void exactSourcePartGraphPivotsAndCubesArePreserved() {
        ModelPart root = MirrorModel.createBodyLayer().bakeRoot();
        MirrorModel model = new MirrorModel(root);

        assertEquals(20, root.getAllParts().size());
        assertEquals(19, cubes(root).size());
        assertEquals(19, model.sourceParts().size());
        assertSame(root.getChild("frame_inner_right"), model.sourceParts().getFirst());
        assertSame(root.getChild("back_middle"), model.sourceParts().getLast());
        assertSame(root.getChild("back_middle"), model.backMiddle());

        assertPart(root, "back_middle", -5.0F, -7.0F, -1.0F,
                5.0F, 8.0F, 0.0F, 1.0F);
        assertPart(root, "back_right", -7.0F, -6.0F, -1.0F,
                -5.0F, 8.0F, 0.0F, 1.0F);
        assertPart(root, "back_left", 5.0F, -6.0F, -1.0F,
                7.0F, 8.0F, 0.0F, 1.0F);
        assertPart(root, "frame_outer_right", -7.5F, -3.0F, -1.0F,
                -6.5F, 8.0F, 0.0F, 0.0F);
        assertPart(root, "frame_outer_left", 6.5F, -3.0F, -1.0F,
                7.5F, 8.0F, 0.0F, 0.0F);
        assertPart(root, "frame_outer_top", -5.0F, -7.5F, -1.0F,
                5.0F, -6.5F, 0.0F, 0.0F);
        assertPart(root, "frame_inner_top", -3.0F, -4.0F, -1.0F,
                3.0F, -3.0F, 0.0F, 0.0F);
    }

    @Test
    void leftHandPartsKeepTheSourceMirroredUvWinding() throws ReflectiveOperationException {
        ModelPart root = MirrorModel.createBodyLayer().bakeRoot();
        assertEquals(-1.0F, westFaceNormal(cube(root.getChild("frame_outer_right"))).x(), EPSILON);
        assertEquals(1.0F, westFaceNormal(cube(root.getChild("frame_outer_left"))).x(), EPSILON);
        assertEquals(-1.0F, westFaceNormal(cube(root.getChild("back_right"))).x(), EPSILON);
        assertEquals(1.0F, westFaceNormal(cube(root.getChild("back_left"))).x(), EPSILON);
    }

    private static void assertPart(ModelPart root, String name,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ,
                                   float pivotZ) {
        ModelPart part = root.getChild(name);
        assertEquals(0.0F, part.x, EPSILON);
        assertEquals(0.0F, part.y, EPSILON);
        assertEquals(pivotZ, part.z, EPSILON);
        ModelPart.Cube cube = cube(part);
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
    }

    private static ModelPart.Cube cube(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (path.isEmpty()) {
                cubes.add(cube);
            }
        });
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static Vector3f westFaceNormal(ModelPart.Cube cube) throws ReflectiveOperationException {
        Object[] polygons = polygons(cube);
        for (Field field : polygons[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons[2]);
            if (value instanceof Vector3f normal) {
                return normal;
            }
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }

    private static Object[] polygons(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        return (Object[]) polygonsField.get(cube);
    }
}
