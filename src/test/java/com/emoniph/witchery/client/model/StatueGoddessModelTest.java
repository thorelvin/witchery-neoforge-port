package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class StatueGoddessModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceNinePartGeometryAndFixedArmPoseArePreserved() {
        ModelPart root = StatueGoddessModel.createBodyLayer().bakeRoot();
        StatueGoddessModel model = new StatueGoddessModel(root);
        assertEquals(10, root.getAllParts().size());
        assertEquals(9, cubes(root).size());
        assertCube(root, "head", 0, 0, 0, -4, -8, -4, 4, 0, 4);
        assertCube(root, "cleave", -4, 3, -2, 0, -2, -2, 8, 2, 2);
        assertEquals(-(float) Math.PI / 4, root.getChild("cleave").xRot, EPSILON);
        assertCube(root, "body", 0, 0, 0, -4, 0, -2, 4, 12, 2);
        assertCube(root, "right_arm", -5, 2, 0, -3, -2, -2, 1, 10, 2);
        assertCube(root, "left_arm", 5, 2, 0, -1, -2, -2, 3, 10, 2);
        assertCube(root, "right_leg", -2, 12, 0, -2, 0, -2, 2, 12, 2);
        assertCube(root, "left_leg", 2, 12, 0, -2, 0, -2, 2, 12, 2);
        assertCube(root, "dress_lower", 0, 17, 0, -6, 0, -4, 6, 7, 4);
        assertCube(root, "dress_middle", 0, 12, 0, -5, 0, -3, 5, 5, 3);

        model.setupSourcePose();
        assertArm(root.getChild("left_arm"), -0.6F, -0.6F, -0.1F);
        assertArm(root.getChild("right_arm"), -0.6F, 0.6F, 0.1F);
    }

    @Test
    void sourceMirrorAssignmentsAfterAddBoxLeaveEveryCubeUnmirrored() throws ReflectiveOperationException {
        for (ModelPart.Cube cube : cubes(StatueGoddessModel.createBodyLayer().bakeRoot())) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    private static void assertCube(ModelPart root, String name,
                                   float px, float py, float pz,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(px, part.x, EPSILON, name + " pivotX");
        assertEquals(py, part.y, EPSILON, name + " pivotY");
        assertEquals(pz, part.z, EPSILON, name + " pivotZ");
        ModelPart.Cube cube = cubes(part).getFirst();
        assertEquals(minX, cube.minX, EPSILON, name + " minX");
        assertEquals(minY, cube.minY, EPSILON, name + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, name + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, name + " maxZ");
    }

    private static void assertArm(ModelPart arm, float xRot, float yRot, float zRot) {
        assertEquals(xRot, arm.xRot, EPSILON);
        assertEquals(yRot, arm.yRot, EPSILON);
        assertEquals(zRot, arm.zRot, EPSILON);
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
