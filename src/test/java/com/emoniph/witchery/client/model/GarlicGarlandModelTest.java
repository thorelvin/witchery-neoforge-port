package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GarlicGarlandModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void exactSourcePartGraphPivotsCubesAndGarlicScalingArePreserved() {
        ModelPart root = GarlicGarlandModel.createBodyLayer().bakeRoot();
        GarlicGarlandModel model = new GarlicGarlandModel(root);

        assertEquals(30, root.getAllParts().size());
        assertEquals(29, cubes(root).size());
        assertGarlic(root.getChild("garlic_a"), -5.0F, 0.0F);
        assertGarlic(root.getChild("garlic_b"), -2.5F, 1.5F);
        assertGarlic(root.getChild("garlic_c"), 0.0F, 0.0F);
        assertGarlic(root.getChild("garlic_d"), 2.5F, 1.5F);
        assertGarlic(root.getChild("garlic_e"), 5.0F, 0.0F);
        for (ModelPart garlic : model.garlicRoots()) {
            assertEquals(GarlicGarlandModel.GARLIC_SCALE, garlic.xScale, EPSILON);
            assertEquals(GarlicGarlandModel.GARLIC_SCALE, garlic.yScale, EPSILON);
            assertEquals(GarlicGarlandModel.GARLIC_SCALE, garlic.zScale, EPSILON);
        }
    }

    @Test
    void stringsKeepTheirExactDeflationPivotsAndAngles() {
        ModelPart root = GarlicGarlandModel.createBodyLayer().bakeRoot();
        assertString(root, "string_1", -5.4F, -0.3F, GarlicGarlandModel.STRING_ANGLE);
        assertString(root, "string_2", -3.0F, 1.8F, -GarlicGarlandModel.STRING_ANGLE);
        assertString(root, "string_3", -0.4F, -0.3F, GarlicGarlandModel.STRING_ANGLE);
        assertString(root, "string_4", 2.0F, 1.8F, -GarlicGarlandModel.STRING_ANGLE);
    }

    @Test
    void allSourceCubesRemainUnmirroredAndUseTheThirtyTwoPixelSheet()
            throws ReflectiveOperationException {
        ModelPart root = GarlicGarlandModel.createBodyLayer().bakeRoot();
        for (ModelPart.Cube cube : cubes(root)) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }

        assertUvBounds(cube(root.getChild("garlic_c")), 0, 4, 0, 3);
        assertUvBounds(cube(root.getChild("garlic_c").getChild("garlic_1")), 0, 12, 3, 7);
        assertUvBounds(cube(root.getChild("garlic_c").getChild("garlic_2")), 0, 20, 7, 13);
        assertUvBounds(cube(root.getChild("garlic_c").getChild("garlic_3")), 0, 28, 13, 23);
        assertUvBounds(cube(root.getChild("garlic_c").getChild("garlic_4")), 0, 16, 23, 28);
        assertUvBounds(cube(root.getChild("string_1")), 6, 16, 0, 2);
    }

    private static void assertGarlic(ModelPart garlic, float x, float y) {
        assertEquals(x, garlic.x, EPSILON);
        assertEquals(y, garlic.y, EPSILON);
        assertEquals(7.0F, garlic.z, EPSILON);
        assertCube(cube(garlic), -0.5F, 0.0F, -0.5F, 0.5F, 2.0F, 0.5F);
        assertCube(cube(garlic.getChild("garlic_1")), -1.5F, 2.0F, -1.5F, 1.5F, 3.0F, 1.5F);
        assertCube(cube(garlic.getChild("garlic_2")), -2.5F, 3.0F, -2.5F, 2.5F, 4.0F, 2.5F);
        assertCube(cube(garlic.getChild("garlic_3")), -3.5F, 4.0F, -3.5F, 3.5F, 7.0F, 3.5F);
        assertCube(cube(garlic.getChild("garlic_4")), -2.0F, 7.0F, -2.0F, 2.0F, 8.0F, 2.0F);
    }

    private static void assertString(ModelPart root, String name, float x, float y, float zRot) {
        ModelPart string = root.getChild(name);
        assertEquals(x, string.x, EPSILON);
        assertEquals(y, string.y, EPSILON);
        assertEquals(7.0F, string.z, EPSILON);
        assertEquals(zRot, string.zRot, EPSILON);
        ModelPart.Cube cube = cube(string);
        assertCube(cube, 0.0F, -0.5F, -0.5F, 4.0F, 0.5F, 0.5F);
        try {
            float[] bounds = renderedVertexBounds(cube);
            assertEquals(0.4F, bounds[0], EPSILON);
            assertEquals(-0.1F, bounds[1], EPSILON);
            assertEquals(-0.1F, bounds[2], EPSILON);
            assertEquals(3.6F, bounds[3], EPSILON);
            assertEquals(0.1F, bounds[4], EPSILON);
            assertEquals(0.1F, bounds[5], EPSILON);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertCube(ModelPart.Cube cube, float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
    }

    private static ModelPart.Cube cube(ModelPart part) {
        List<ModelPart.Cube> cubes = directCubes(part);
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }

    private static List<ModelPart.Cube> directCubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (path.isEmpty()) cubes.add(cube);
        });
        return cubes;
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static void assertUvBounds(ModelPart.Cube cube, float minU, float maxU,
                                       float minV, float maxV) throws ReflectiveOperationException {
        float[] actual = uvBounds(cube);
        assertEquals(minU / 32.0F, actual[0], EPSILON);
        assertEquals(maxU / 32.0F, actual[1], EPSILON);
        assertEquals(minV / 32.0F, actual[2], EPSILON);
        assertEquals(maxV / 32.0F, actual[3], EPSILON);
    }

    private static Vector3f westFaceNormal(ModelPart.Cube cube) throws ReflectiveOperationException {
        for (Field field : polygons(cube)[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons(cube)[2]);
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }

    private static float[] uvBounds(ModelPart.Cube cube) throws ReflectiveOperationException {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (Object polygon : polygons(cube)) {
            for (Field field : polygon.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(polygon);
                if (!(value instanceof Object[] vertices)) continue;
                for (Object vertex : vertices) {
                    Field u = vertex.getClass().getDeclaredField("u");
                    Field v = vertex.getClass().getDeclaredField("v");
                    u.setAccessible(true);
                    v.setAccessible(true);
                    float vertexU = u.getFloat(vertex);
                    float vertexV = v.getFloat(vertex);
                    minU = Math.min(minU, vertexU);
                    maxU = Math.max(maxU, vertexU);
                    minV = Math.min(minV, vertexV);
                    maxV = Math.max(maxV, vertexV);
                }
            }
        }
        return new float[]{minU, maxU, minV, maxV};
    }

    private static float[] renderedVertexBounds(ModelPart.Cube cube)
            throws ReflectiveOperationException {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (Object polygon : polygons(cube)) {
            for (Field field : polygon.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(polygon);
                if (!(value instanceof Object[] vertices)) continue;
                for (Object vertex : vertices) {
                    Field x = vertex.getClass().getDeclaredField("x");
                    Field y = vertex.getClass().getDeclaredField("y");
                    Field z = vertex.getClass().getDeclaredField("z");
                    x.setAccessible(true);
                    y.setAccessible(true);
                    z.setAccessible(true);
                    float vx = x.getFloat(vertex);
                    float vy = y.getFloat(vertex);
                    float vz = z.getFloat(vertex);
                    minX = Math.min(minX, vx);
                    minY = Math.min(minY, vy);
                    minZ = Math.min(minZ, vz);
                    maxX = Math.max(maxX, vx);
                    maxY = Math.max(maxY, vy);
                    maxZ = Math.max(maxZ, vz);
                }
            }
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static Object[] polygons(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        return (Object[]) polygonsField.get(cube);
    }
}
