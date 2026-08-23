package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DeathArmorModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceBaseBipedAndTenClothingChildrenArePreserved() {
        ModelPart root = DeathArmorModel.createLayer().bakeRoot();
        DeathArmorModel model = new DeathArmorModel(root);
        assertEquals(18, root.getAllParts().size());
        assertEquals(17, cubes(root).size());

        assertEquals(1, cubesAtPath(root.getChild("head"), "").size());
        assertEquals(1, cubesAtPath(model.hat, "").size());
        assertPart(root.getChild("head").getChild("death_head"), 0, 0, 0,
                -4.5F, -8.5F, -4.5F, 4.5F, 0.5F, 4.5F);
        assertPart(root.getChild("head").getChild("cowl"), 0, 0, 0,
                -4.9F, -8.9F, -4.9F, 4.9F, 1.9F, 4.9F);
        assertPart(root.getChild("body").getChild("death_body"), 0, 0, 0,
                -4, 0, -2, 4, 12, 2);
        assertPart(root.getChild("body").getChild("robe"), 0, 0, 0,
                -4.6F, -0.1F, -2.6F, 4.6F, 22.1F, 2.6F);
        assertPart(root.getChild("right_arm").getChild("death_arm"), 0, 0, 0,
                -3.05F, -2.05F, -2.05F, 1.05F, 10.05F, 2.05F);
        assertPart(root.getChild("left_arm").getChild("death_arm"), 0, 0, 0,
                -1.05F, -2.05F, -2.05F, 3.05F, 10.05F, 2.05F);
        assertPart(root.getChild("right_leg").getChild("death_leg"), 0, 0, 0,
                -2.05F, -0.05F, -2.05F, 2.05F, 12.05F, 2.05F);
        assertPart(root.getChild("left_leg").getChild("death_leg"), 0, 0, 0,
                -2.05F, -0.05F, -2.05F, 2.05F, 12.05F, 2.05F);
    }

    @Test
    void onlyTheTwoInheritedLegacyBipedCubesAreMirrored() throws ReflectiveOperationException {
        long mirrored = cubes(DeathArmorModel.createLayer().bakeRoot()).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(2, mirrored);
    }

    private static void assertPart(ModelPart part, float x, float y, float z,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertRenderedBounds(cubesAtPath(part, "").getFirst(), minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
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

    private static void assertRenderedBounds(ModelPart.Cube cube,
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

    private static float[] renderedBounds(ModelPart.Cube cube) {
        try {
            Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
            polygonsField.setAccessible(true);
            Object[] polygons = (Object[]) polygonsField.get(cube);
            float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (Object polygon : polygons) {
                for (Field polygonField : polygon.getClass().getDeclaredFields()) {
                    polygonField.setAccessible(true);
                    Object value = polygonField.get(polygon);
                    if (value == null || !polygonField.getType().isArray()) continue;
                    for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++) {
                        Object vertex = java.lang.reflect.Array.get(value, i);
                        Float x = null;
                        Float y = null;
                        Float z = null;
                        for (Field vertexField : vertex.getClass().getDeclaredFields()) {
                            vertexField.setAccessible(true);
                            if (vertexField.getType() == float.class) {
                                switch (vertexField.getName()) {
                                    case "x" -> x = vertexField.getFloat(vertex);
                                    case "y" -> y = vertexField.getFloat(vertex);
                                    case "z" -> z = vertexField.getFloat(vertex);
                                    default -> {
                                    }
                                }
                            }
                        }
                        if (x != null && y != null && z != null) {
                            bounds[0] = Math.min(bounds[0], x);
                            bounds[1] = Math.min(bounds[1], y);
                            bounds[2] = Math.min(bounds[2], z);
                            bounds[3] = Math.max(bounds[3], x);
                            bounds[4] = Math.max(bounds[4], y);
                            bounds[5] = Math.max(bounds[5], z);
                        }
                    }
                }
            }
            return bounds;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
