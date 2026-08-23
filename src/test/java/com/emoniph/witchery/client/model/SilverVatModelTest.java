package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SilverVatModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphAndEveryCubeCoordinateArePreserved() {
        ModelPart root = SilverVatModel.createBodyLayer().bakeRoot();

        assertEquals(22, root.getAllParts().size());
        assertPart(root, "base", -6, 23, -6, 12, 1, 12);
        assertPart(root, "side_right", -7, 16, -6, 1, 8, 12);
        assertPart(root, "side_front", -7, 16, -7, 14, 8, 1);
        assertPart(root, "side_back", -7, 16, 6, 14, 8, 1);
        assertPart(root, "side_left", 6, 16, -6, 1, 8, 12);

        assertPart(root, "spout_lower_west", 4.2F, 16, -0.5F, 1, 1, 1);
        assertPart(root, "spout_upper_west", 4, 14, -1.5F, 4, 2, 3);
        assertPart(root, "spout_lower_east", -5.2F, 16, -0.5F, 1, 1, 1);
        assertPart(root, "spout_upper_east", -8, 14, -1.5F, 4, 2, 3);
        assertPart(root, "spout_lower_north", -0.5F, 16, -5.2F, 1, 1, 1);
        assertPart(root, "spout_upper_north", -1.5F, 14, -8, 3, 2, 4);
        assertPart(root, "spout_lower_south", -0.5F, 16, 4.2F, 1, 1, 1);
        assertPart(root, "spout_upper_south", -1.5F, 14, 4, 3, 2, 4);

        assertPart(root, "silver_1", -2.2F, 19.3F, -3.9F, 1, 1, 1);
        assertPart(root, "silver_2", 1.6F, 19, -2.1F, 2, 1, 1);
        assertPart(root, "silver_3", -3.8F, 19.1F, 3.1F, 1, 1, 2);
        assertPart(root, "silver_4", -3.4F, 18.8F, 0.9F, 1, 1, 1);
        assertPart(root, "silver_5", 1.6F, 19, -0.1F, 1, 1, 1);
        assertPart(root, "silver_6", -4.6F, 19.1F, -1.6F, 2, 1, 1);
        assertPart(root, "silver_7", -0.5F, 19, 2, 1, 1, 1);
        assertPart(root, "silver_8", -1.2F, 19, -0.3F, 1, 1, 1);
        assertEquals(21, cubes(root).size());
    }

    @Test
    void spoutDirectionsMatchTheSourceNeighborChecksAfterModelFlip() {
        SilverVatModel model = new SilverVatModel(SilverVatModel.createBodyLayer().bakeRoot());

        assertEquals(4.0F, model.spout(Direction.WEST).iterator().next().x, EPSILON);
        assertEquals(-8.0F, model.spout(Direction.EAST).iterator().next().x, EPSILON);
        assertEquals(-8.0F, model.spout(Direction.NORTH).iterator().next().z, EPSILON);
        assertEquals(4.0F, model.spout(Direction.SOUTH).iterator().next().z, EPSILON);
    }

    @Test
    void everySourceCubeRemainsUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(SilverVatModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void liquidPlanePreservesSourceSizeAndPivot() {
        ModelPart liquid = SilverVatModel.createLiquidLayer().bakeRoot().getChild("liquid");
        assertEquals(2, SilverVatModel.createLiquidLayer().bakeRoot().getAllParts().size());
        assertEquals(0.0F, liquid.x, EPSILON);
        assertEquals(19.2F, liquid.y, EPSILON);
        assertEquals(0.0F, liquid.z, EPSILON);
        ModelPart.Cube cube = cubes(liquid).getFirst();
        assertCube(cube, -8, 0, -8, 8, 0, 8, "liquid");
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float sizeX, float sizeY, float sizeZ) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size(), name + " cube count");
        assertCube(cubes.getFirst(), 0, 0, 0, sizeX, sizeY, sizeZ, name);
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
