package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3fc;

final class LegacyModelTestSupport {
    private static final float EPSILON = 0.0001F;

    private LegacyModelTestSupport() {
    }

    static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    static List<ModelPart.Cube> cubesAtPath(ModelPart part, String wantedPath) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (wantedPath.equals(path)) cubes.add(cube);
        });
        return cubes;
    }

    static long mirroredCubeCount(ModelPart part) {
        return cubes(part).stream().filter(cube -> westFaceNormalX(cube) > 0.0F).count();
    }

    static void assertRenderedBounds(ModelPart.Cube cube,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ) {
        float[] bounds = renderedBounds(cube);
        assertEquals(minX, bounds[0], EPSILON);
        assertEquals(minY, bounds[1], EPSILON);
        assertEquals(minZ, bounds[2], EPSILON);
        assertEquals(maxX, bounds[3], EPSILON);
        assertEquals(maxY, bounds[4], EPSILON);
        assertEquals(maxZ, bounds[5], EPSILON);
    }

    static void assertUvBounds(ModelPart.Cube cube, float minU, float minV, float maxU, float maxV) {
        float actualMinU = Float.POSITIVE_INFINITY;
        float actualMinV = Float.POSITIVE_INFINITY;
        float actualMaxU = Float.NEGATIVE_INFINITY;
        float actualMaxV = Float.NEGATIVE_INFINITY;
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                actualMinU = Math.min(actualMinU, vertex.u());
                actualMinV = Math.min(actualMinV, vertex.v());
                actualMaxU = Math.max(actualMaxU, vertex.u());
                actualMaxV = Math.max(actualMaxV, vertex.v());
            }
        }
        assertEquals(minU, actualMinU, EPSILON, "min U");
        assertEquals(minV, actualMinV, EPSILON, "min V");
        assertEquals(maxU, actualMaxU, EPSILON, "max U");
        assertEquals(maxV, actualMaxV, EPSILON, "max V");
    }

    private static float westFaceNormalX(ModelPart.Cube cube) {
        try {
            Object[] polygons = polygons(cube);
            for (Field field : polygons[2].getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(polygons[2]) instanceof Vector3fc normal) return normal.x();
            }
            throw new NoSuchFieldException("Baked polygon has no vector normal");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static float[] renderedBounds(ModelPart.Cube cube) {
        try {
            float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (Object polygon : polygons(cube)) {
                for (Field polygonField : polygon.getClass().getDeclaredFields()) {
                    polygonField.setAccessible(true);
                    Object value = polygonField.get(polygon);
                    if (value == null || !polygonField.getType().isArray()) continue;
                    for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++) {
                        Object vertex = java.lang.reflect.Array.get(value, i);
                        Float x = coordinate(vertex, "x");
                        Float y = coordinate(vertex, "y");
                        Float z = coordinate(vertex, "z");
                        if (x == null || y == null || z == null) continue;
                        bounds[0] = Math.min(bounds[0], x);
                        bounds[1] = Math.min(bounds[1], y);
                        bounds[2] = Math.min(bounds[2], z);
                        bounds[3] = Math.max(bounds[3], x);
                        bounds[4] = Math.max(bounds[4], y);
                        bounds[5] = Math.max(bounds[5], z);
                    }
                }
            }
            return bounds;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Object[] polygons(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        return (Object[]) polygonsField.get(cube);
    }

    private static Float coordinate(Object vertex, String name) throws ReflectiveOperationException {
        for (Field field : vertex.getClass().getDeclaredFields()) {
            if (field.getType() == float.class && name.equals(field.getName())) {
                field.setAccessible(true);
                return field.getFloat(vertex);
            }
        }
        return null;
    }
}
