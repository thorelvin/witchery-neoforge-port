package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DemonEntityModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceEightPartFourteenCubeGraphAndWingPosesArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = DemonEntityModel.createBodyLayer().bakeRoot();
        assertEquals(9, root.getAllParts().size());
        assertEquals(14, cubes(root).size());
        assertPose(root, "head", 0, -9, 0, 0, 0, 0, 7);
        assertPose(root, "body", 0, -9, 0, 0, 0, 0, 1);
        assertPose(root, "right_arm", -5, -7, 0, 0, 0, 0, 1);
        assertPose(root, "left_arm", 5, -7, 0, 0, 0, 0, 1);
        assertPose(root, "right_leg", -2, 4, 0, 0, 0, 0, 1);
        assertPose(root, "left_leg", 2, 4, 0, 0, 0, 0, 1);
        assertPose(root, "wing_right", 1, -8, 3, 0.3047198F, -0.6698132F, -0.6283185F, 1);
        assertPose(root, "wing_left", -1, -8, 3, -0.3047198F, 3.811406F, 0.6283185F, 1);
        for (ModelPart.Cube cube : cubes(root)) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void walkingRollIsNotIncorrectlyAppliedInsideTheModelGraph() {
        ModelPart root = DemonEntityModel.createBodyLayer().bakeRoot();
        DemonEntityModel model = new DemonEntityModel(root);
        DemonEntityModel.DemonRenderState state = new DemonEntityModel.DemonRenderState();
        state.walkAnimationPos = 4.0F;
        state.walkAnimationSpeed = 0.7F;
        model.setupAnim(state);
        assertEquals(0.0F, root.zRot, EPSILON);
    }

    private static void assertPose(ModelPart root, String name, float x, float y, float z,
                                   float xRot, float yRot, float zRot, int cubeCount) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        assertEquals(xRot, part.xRot, EPSILON, name + " xRot");
        assertEquals(yRot, part.yRot, EPSILON, name + " yRot");
        assertEquals(zRot, part.zRot, EPSILON, name + " zRot");
        assertEquals(cubeCount, cubes(part).size(), name + " cube count");
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
