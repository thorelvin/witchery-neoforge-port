package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WolfStatueModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void allTwelveSourcePartsPreserveTheirPivotsAndRotations() {
        ModelPart root = WolfStatueModel.createBodyLayer().bakeRoot();
        assertEquals(13, root.getAllParts().size());
        assertEquals(12, cubes(root).size());
        assertPose(root, "head", -0.5F, 13.5F, -7, 0, 0, 0);
        assertPose(root, "body", 0.5F, 14, 2, 1.570796F, 0, 0);
        assertPose(root, "mane", -0.5F, 14, -3, 1.570796F, 0, 0);
        assertPose(root, "rear_left_leg", -2, 16, 7, 0, 0, 0);
        assertPose(root, "rear_right_leg", 1, 16, 7, 0, 0, 0);
        assertPose(root, "front_left_leg", -2, 16, -4, 0, 0, 0);
        assertPose(root, "front_right_leg", 1, 16, -4, 0, 0, 0);
        assertPose(root, "tail", -0.5F, 12, 8, 1.130069F, 0, 0);
        assertPose(root, "left_ear", -0.5F, 13.5F, -7, 0, 0, 0);
        assertPose(root, "right_ear", -0.5F, 13.5F, -7, 0, 0, 0);
        assertPose(root, "nose", 0, 13.5F, -7, 0, 0, 0);
        assertPose(root, "pedestal", -4.5F, 23, -5, 0, 0, 0);
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(WolfStatueModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPose(ModelPart root, String name, float x, float y, float z,
                                   float xRot, float yRot, float zRot) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        assertEquals(xRot, part.xRot, EPSILON, name + " xRot");
        assertEquals(yRot, part.yRot, EPSILON, name + " yRot");
        assertEquals(zRot, part.zRot, EPSILON, name + " zRot");
        assertEquals(1, cubes(part).size(), name + " cube count");
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
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
