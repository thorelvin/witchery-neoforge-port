package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SpinningWheelModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphGeometryPivotsAndRotationsArePreserved() {
        ModelPart root = SpinningWheelModel.createBodyLayer().bakeRoot();

        assertEquals(11, root.getAllParts().size());
        assertPart(root, "seat", 0, 18, 0, -2, -1, -7, 2, 0, 7);
        assertRotation(root.getChild("seat"), 0.2602503F, 0, 0);
        assertPart(root, "leg_back_right", -1, 16, 5, -1, 0, 0, 0, 9, 1);
        assertRotation(root.getChild("leg_back_right"), 0.1745329F, 0, 0.1745329F);
        assertPart(root, "leg_back_left", 1, 16, 5, 0, 0, 0, 1, 9, 1);
        assertRotation(root.getChild("leg_back_left"), 0.1745329F, 0, -0.1745329F);
        assertPart(root, "leg_front_right", -1, 19, -6, -1, 0, 0, 0, 6, 1);
        assertRotation(root.getChild("leg_front_right"), -0.1745329F, 0, 0.1745329F);
        assertPart(root, "leg_front_left", 1, 19, -6, 0, 0, 0, 1, 6, 1);
        assertRotation(root.getChild("leg_front_left"), -0.1745329F, 0, -0.1745329F);
        assertPart(root, "thread", 0, 12, 5, -1, -3, -1, 1, 0, 1);
        assertPart(root, "thread_pole", 0, 12, 5, -0.5F, 0, -0.5F, 0.5F, 4, 0.5F);
        assertPart(root, "arm_right", -1, 18, -2, -0.5F, -7, -0.5F, 0.5F, 0, 0.5F);
        assertRotation(root.getChild("arm_right"), 0.2268928F, 0, 0);
        assertPart(root, "arm_left", 1, 18, -2, -0.5F, -7, -0.5F, 0.5F, 0, 0.5F);
        assertRotation(root.getChild("arm_left"), 0.2268928F, 0, 0);

        ModelPart wheel = root.getChild("wheel");
        assertPose(wheel, 0, 12, -3.5F);
        List<ModelPart.Cube> wheelCubes = cubes(wheel);
        assertEquals(5, wheelCubes.size());
        assertCube(wheelCubes.get(0), 0, -3, -3, 0, 3, 3, "spokes");
        assertCube(wheelCubes.get(1), -0.5F, -4, -3, 0.5F, -3, 3, "top");
        assertCube(wheelCubes.get(2), -0.5F, 3, -3, 0.5F, 4, 3, "bottom");
        assertCube(wheelCubes.get(3), -0.5F, -4, 3, 0.5F, 4, 4, "back");
        assertCube(wheelCubes.get(4), -0.5F, -4, -4, 0.5F, 4, -3, "front");
        assertEquals(14, cubes(root).size());
    }

    @Test
    void sourceMirrorTimingIsPreserved() throws ReflectiveOperationException {
        ModelPart root = SpinningWheelModel.createBodyLayer().bakeRoot();
        for (String name : List.of("seat", "leg_back_right", "leg_back_left", "leg_front_right",
                "leg_front_left", "thread", "thread_pole", "arm_right", "arm_left")) {
            assertEquals(-1.0F, westFaceNormal(cubes(root.getChild(name)).getFirst()).x(), EPSILON, name);
        }
        for (ModelPart.Cube cube : cubes(root.getChild("wheel"))) {
            assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON, "wheel");
        }
    }

    @Test
    void sourceWallClockAnimationAndIdleResetArePreserved() {
        ModelPart root = SpinningWheelModel.createBodyLayer().bakeRoot();
        SpinningWheelModel model = new SpinningWheelModel(root);

        model.setupAnimation(true, 25_000L);
        assertEquals(-333.0F, model.wheel().xRot, EPSILON);
        assertEquals(140.0F, model.thread().yRot, EPSILON);

        model.setupAnimation(false, 25_000L);
        assertEquals(0.0F, model.wheel().xRot, EPSILON);
        assertEquals(0.0F, model.thread().yRot, EPSILON);
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
