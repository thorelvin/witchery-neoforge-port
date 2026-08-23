package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FumeFunnelModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphAndEveryCubeCoordinateArePreserved() {
        ModelPart root = FumeFunnelModel.createBodyLayer().bakeRoot();
        List<PartSpec> expected = List.of(
                new PartSpec("base", -6, 23, -6, 12, 1, 12),
                new PartSpec("body", -5, 12, -5, 10, 11, 10),
                new PartSpec("tube_left", -10, 17, -1, 5, 2, 2),
                new PartSpec("tube_right", 5, 18, 1, 5, 2, 2),
                new PartSpec("pipe_top2", -4, 8, -3, 1, 1, 6),
                new PartSpec("pipe_top3", -3, 8, -3, 11, 1, 1),
                new PartSpec("pipe_top4", 7, 9, -3, 1, 11, 1),
                new PartSpec("pipe_top5", 5, 18, -4, 2, 3, 3),
                new PartSpec("pipe_bottom1", -7, 13, -3, 2, 1, 1),
                new PartSpec("pipe_bottom2", -7, 20, -7, 1, 1, 4),
                new PartSpec("pipe_bottom3", -6, 20, -7, 5, 1, 1),
                new PartSpec("pipe_bottom4", -2, 21, -7, 1, 3, 1),
                new PartSpec("pipe_top1", -4, 8, 3, 1, 3, 1),
                new PartSpec("top1", -6, 11, -6, 12, 1, 12),
                new PartSpec("top2", -3, 10, -3, 6, 1, 6),
                new PartSpec("pipe_bottom5", -7, 14, -3, 1, 7, 1),
                new PartSpec("filter_left", -4, 14, -7, 1, 1, 2),
                new PartSpec("filter_right", 3, 14, -7, 1, 1, 2),
                new PartSpec("filter_mid", -3, 14, -7, 6, 1, 1),
                new PartSpec("filter_case", -2, 13, -8, 4, 3, 2),
                new PartSpec("chimney", -2, 14, 3, 4, 10, 4),
                new PartSpec("chimney_top", -3, 11, 2, 6, 3, 6));

        assertEquals(23, root.getAllParts().size());
        for (PartSpec spec : expected) {
            assertPart(root, spec);
        }
        assertEquals(22, cubes(root).size());
    }

    @Test
    void sourceMirrorAssignmentsOccurAfterCubeConstruction() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(FumeFunnelModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void renderedPartGroupsMatchSourceConditionalSubsets() {
        FumeFunnelModel model = new FumeFunnelModel(FumeFunnelModel.createBodyLayer().bakeRoot());
        assertEquals(4, model.wideBodyParts().size());
        assertEquals(6, model.leftConnectionParts().size());
        assertEquals(6, model.rightConnectionParts().size());
        assertEquals(4, model.filterParts().size());
        assertEquals(2, model.chimneyParts().size());
    }

    private static void assertPart(ModelPart root, PartSpec spec) {
        ModelPart part = root.getChild(spec.name());
        assertEquals(spec.x(), part.x, EPSILON, spec.name() + " x");
        assertEquals(spec.y(), part.y, EPSILON, spec.name() + " y");
        assertEquals(spec.z(), part.z, EPSILON, spec.name() + " z");
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size(), spec.name() + " cube count");
        ModelPart.Cube cube = cubes.getFirst();
        assertEquals(0.0F, cube.minX, EPSILON, spec.name() + " minX");
        assertEquals(0.0F, cube.minY, EPSILON, spec.name() + " minY");
        assertEquals(0.0F, cube.minZ, EPSILON, spec.name() + " minZ");
        assertEquals(spec.sizeX(), cube.maxX, EPSILON, spec.name() + " maxX");
        assertEquals(spec.sizeY(), cube.maxY, EPSILON, spec.name() + " maxY");
        assertEquals(spec.sizeZ(), cube.maxZ, EPSILON, spec.name() + " maxZ");
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

    private record PartSpec(String name, float x, float y, float z,
                            float sizeX, float sizeY, float sizeZ) {
    }
}
