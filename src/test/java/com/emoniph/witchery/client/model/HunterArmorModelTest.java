package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class HunterArmorModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceVisibleSlotGeometryUsesChestAndLegModelDeformations() {
        ModelPart root = HunterArmorModel.createLayer().bakeRoot();
        HunterArmorModel model = new HunterArmorModel(root);
        assertEquals(12, root.getAllParts().size());
        assertEquals(11, cubes(root).size());

        assertRenderedBounds(cubesAtPath(root.getChild("head"), "").getFirst(),
                -4.4F, -8.4F, -4.4F, 4.4F, 0.4F, 4.4F);
        assertRenderedBounds(cubesAtPath(model.hat, "").getFirst(),
                -4.9F, -8.9F, -4.9F, 4.9F, 0.9F, 4.9F);
        assertPart(root.getChild("head").getChild("hat_brim"), 0, -6, 0,
                -6.82F, -0.32F, -6.82F, 6.82F, 1.32F, 6.82F);
        assertPart(root.getChild("head").getChild("hat_brim").getChild("hat_mid"), 0, -2, 0,
                -4.52F, -0.52F, -4.52F, 4.52F, 2.52F, 4.52F);
        assertPart(root.getChild("head").getChild("hat_brim").getChild("hat_mid").getChild("hat_top"),
                0, -2, 0, -4.02F, -0.52F, -4.02F, 4.02F, 2.52F, 4.02F);
        assertPart(root.getChild("body").getChild("coat"), 0, 12, 0,
                -5.2F, 0.3F, -2.7F, 5.2F, 9.7F, 2.7F);
        assertRenderedBounds(cubesAtPath(root.getChild("right_leg"), "").getFirst(),
                -2.01F, -0.01F, -2.01F, 2.01F, 12.01F, 2.01F);
        assertRenderedBounds(cubesAtPath(root.getChild("left_leg"), "").getFirst(),
                -2.01F, -0.01F, -2.01F, 2.01F, 12.01F, 2.01F);
    }

    @Test
    void onlyTheInheritedLeftArmAndSlimLeftLegAreMirrored() throws ReflectiveOperationException {
        long mirrored = cubes(HunterArmorModel.createLayer().bakeRoot()).stream().filter(cube -> {
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
