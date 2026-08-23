package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class KettleModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphGeometryPivotsAndRotationsArePreserved() {
        ModelPart root = KettleModel.createBodyLayer().bakeRoot();

        assertEquals(21, root.getAllParts().size());
        assertPart(root, "side_front", -5, 18, -5, 0, 0, 0, 9, 6, 1);
        assertPart(root, "side_back", -4, 18, 4, 0, 0, 0, 9, 6, 1);
        assertPart(root, "side_left", -5, 18, 5, 0, 0, 0, 9, 6, 1);
        assertRotation(root.getChild("side_left"), 0, 1.570796F, 0);
        assertPart(root, "side_right", 4, 18, 4, 0, 0, 0, 9, 6, 1);
        assertRotation(root.getChild("side_right"), 0, 1.570796F, 0);
        assertPart(root, "side_bottom", -4, 23, -4, 0, 0, 0, 8, 1, 8);
        assertPart(root, "crossbar", -8, 8.05F, -1, -4, 0, 0, 20, 2, 2);

        for (int i = 0; i < 8; i++) {
            assertPart(root, "liquid_" + i, -4, 20, -4, 0, 0, 0, 8, 0, 8);
        }

        assertPart(root, "chain_lf", 0, 9, 0, 0, -0.5F, 0, 11, 0.5F, 0);
        assertRotation(root.getChild("chain_lf"), 0, -2.75F, -1.1F);
        assertPart(root, "chain_lb", 0, 9, 0, 0, -0.5F, 0, 11, 0.5F, 0);
        assertRotation(root.getChild("chain_lb"), 0, 0.4F, 1.1F);
        assertPart(root, "chain_rf", 0, 9, 0, 0, -0.5F, 0, 11, 0.5F, 0);
        assertRotation(root.getChild("chain_rf"), 0, 0.4F, 2.05F);
        assertPart(root, "chain_rb", 0, 9, 0, 0, -0.5F, 0, 11, 0.5F, 0);
        assertRotation(root.getChild("chain_rb"), 0, -0.4F, 1.1F);

        assertBottle(root.getChild("bottle1"), -4, 13, -6);
        assertBottle(root.getChild("bottle2"), 0, 13, -6);

        List<ModelPart.Cube> allCubes = cubes(root);
        assertEquals(24, allCubes.size());
    }

    @Test
    void mirrorTimingMatchesTheCubesActuallyBuiltByTheSource() throws ReflectiveOperationException {
        ModelPart root = KettleModel.createBodyLayer().bakeRoot();
        for (String name : List.of(
                "side_front", "side_back", "side_left", "side_right", "side_bottom", "crossbar",
                "liquid_0", "liquid_1", "liquid_2", "liquid_3", "liquid_4", "liquid_5", "liquid_6", "liquid_7",
                "chain_lf", "chain_lb", "chain_rf", "chain_rb")) {
            assertEquals(-1.0F, westFaceNormal(cubes(root.getChild(name)).getFirst()).x(), EPSILON, name);
        }
        for (String name : List.of("bottle1", "bottle2")) {
            for (ModelPart.Cube cube : cubes(root.getChild(name))) {
                assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON, name);
            }
        }
    }

    private static void assertBottle(ModelPart bottle, float x, float y, float z) {
        assertPose(bottle, x, y, z);
        List<ModelPart.Cube> cubes = cubes(bottle);
        assertEquals(3, cubes.size());
        assertCube(cubes.get(0), 0, 2, 0, 3, 5, 3, "body");
        assertCube(cubes.get(1), 1, 1, 1, 2, 2, 2, "neck");
        assertCube(cubes.get(2), 0.5F, 0, 0.5F, 2.5F, 1, 2.5F, "top");
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertPose(part, x, y, z);
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size(), name + " cube count");
        assertCube(cubes.getFirst(), minX, minY, minZ, maxX, maxY, maxZ, name);
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

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
    }

    private static void assertRotation(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.xRot, EPSILON);
        assertEquals(y, part.yRot, EPSILON);
        assertEquals(z, part.zRot, EPSILON);
    }

    private static void assertCube(ModelPart.Cube cube,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ, String label) {
        assertEquals(minX, cube.minX, EPSILON, label + " minX");
        assertEquals(minY, cube.minY, EPSILON, label + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, label + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, label + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, label + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, label + " maxZ");
    }
}
