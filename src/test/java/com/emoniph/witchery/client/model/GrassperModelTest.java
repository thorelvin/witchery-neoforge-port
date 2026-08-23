package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GrassperModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void allTenSourcePartsPreserveTheirPivotsAndRotations() {
        ModelPart root = GrassperModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(10, cubes(root).size());
        assertPose(root, "top_stalk", 2, 21, 0, 0, 0, -(float) Math.PI / 6);
        assertPose(root, "right_leaf", 0, 24, 0, 0, (float) Math.PI, (float) Math.PI / 6);
        assertPose(root, "front_leaf", 0, 24, 0, -(float) Math.PI / 6, 0, 0);
        assertPose(root, "back_leaf", 0, 24, 0, -(float) Math.PI / 6, -(float) Math.PI, 0);
        assertPose(root, "left_leaf", 0, 24, 0, 0, 0, -(float) Math.PI / 6);
        assertPose(root, "back_right_petal", 0, 18, 0, -(float) Math.PI / 6, 0, -(float) Math.PI / 4);
        assertPose(root, "bottom_stalk", 0, 24, 0, 0, 0, (float) Math.PI / 6);
        assertPose(root, "front_right_petal", 0, 18, 0, (float) Math.PI / 6, 0, -(float) Math.PI / 4);
        assertPose(root, "back_left_petal", 0, 18, 0, -0.3490659F, 0, (float) Math.PI / 12);
        assertPose(root, "front_left_petal", 0, 18, 0, 0.3490659F, 0, (float) Math.PI / 12);
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(GrassperModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertPose(ModelPart root, String name,
                                   float x, float y, float z, float xRot, float yRot, float zRot) {
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
