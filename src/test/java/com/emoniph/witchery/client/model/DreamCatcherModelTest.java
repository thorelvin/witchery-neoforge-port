package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.emoniph.witchery.blocks.DreamWeaveType;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DreamCatcherModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphAndEveryCubeCoordinateArePreserved() {
        ModelPart root = DreamCatcherModel.createBodyLayer().bakeRoot();

        assertEquals(11, root.getAllParts().size());
        assertPart(root, "frame_left", -4, 10, 7, 1, 8, 1);
        assertPart(root, "frame_right", 3, 10, 7, 1, 8, 1);
        assertPart(root, "frame_top", -3, 10, 7, 6, 1, 1);
        assertPart(root, "frame_bottom", -3, 17, 7, 6, 1, 1);
        assertPart(root, "net_fleet_foot", -3, 11, 8, 6, 6, 0);
        assertPart(root, "net_iron_arm", -3, 11, 8, 6, 6, 0);
        assertPart(root, "net_fasting", -3, 11, 8, 6, 6, 0);
        assertPart(root, "net_nightmares", -3, 11, 8, 6, 6, 0);
        assertPart(root, "net_intensity", -3, 11, 8, 6, 6, 0);
        assertPart(root, "decoration", -4, 18, 7, 8, 6, 0);
        assertEquals(10, cubes(root).size());
    }

    @Test
    void weaveIdsSelectTheSameNetOrderAsTheSourceRegistry() {
        ModelPart root = DreamCatcherModel.createBodyLayer().bakeRoot();
        DreamCatcherModel model = new DreamCatcherModel(root);

        assertSame(root.getChild("net_fleet_foot"), model.net(DreamWeaveType.FLEET_FOOT));
        assertSame(root.getChild("net_iron_arm"), model.net(DreamWeaveType.IRON_ARM));
        assertSame(root.getChild("net_fasting"), model.net(DreamWeaveType.FASTING));
        assertSame(root.getChild("net_nightmares"), model.net(DreamWeaveType.NIGHTMARES));
        assertSame(root.getChild("net_intensity"), model.net(DreamWeaveType.INTENSITY));
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(DreamCatcherModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float sizeX, float sizeY, float sizeZ) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size(), name + " cube count");
        ModelPart.Cube cube = cubes.getFirst();
        assertEquals(0.0F, cube.minX, EPSILON, name + " minX");
        assertEquals(0.0F, cube.minY, EPSILON, name + " minY");
        assertEquals(0.0F, cube.minZ, EPSILON, name + " minZ");
        assertEquals(sizeX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(sizeY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(sizeZ, cube.maxZ, EPSILON, name + " maxZ");
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
