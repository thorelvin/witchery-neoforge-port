package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BroomModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void allTenSourcePartsPreserveGeometryPivotsAndRotations() {
        ModelPart root = BroomModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(10, cubes(root).size());
        assertPose(root, "handle", 0, 11, -5, 1.570796F, 0, 0, 1);
        assertPose(root, "bristle1", -1, 10, 9, 0.1858931F, -0.1487144F, 0, 1);
        assertPose(root, "bristle2", 1, 12, 9, -0.1487144F, 0.1858931F, 0, 1);
        assertPose(root, "bristle3", 1, 10, 9, 0.2230717F, 0.1858931F, 0, 1);
        assertPose(root, "bristle4", 0, 10, 9, 0.2230717F, 0.0743572F, 0, 1);
        assertPose(root, "bristle5", -1, 12, 9, -0.2230717F, -0.1487144F, 0, 1);
        assertPose(root, "bristle6", 0, 11, 9, -0.0371786F, 0.0743572F, 0, 1);
        assertPose(root, "bristle7", 1, 11, 9, -0.0371786F, 0.2230717F, 0, 1);
        assertPose(root, "bristle8", -1, 11, 9, -0.0743572F, -0.1487144F, 0, 1);
        assertPose(root, "bristle9", 0, 12, 9, -0.1858931F, 0, 0, 1);
        assertEquals(-0.5333334F, cubes(root.getChild("bristle9")).getFirst().minX, EPSILON);
    }

    @Test
    void sourceHasNoAgeOrMovementSwayAndEveryCubeIsEffectivelyUnmirrored()
            throws ReflectiveOperationException {
        ModelPart root = BroomModel.createBodyLayer().bakeRoot();
        BroomModel model = new BroomModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.ageInTicks = 1234.0F;
        state.walkAnimationPos = 8.0F;
        state.walkAnimationSpeed = 1.0F;
        model.setupAnim(state);
        for (ModelPart part : root.getAllParts()) {
            assertEquals(0.0F, part.zRot, EPSILON);
        }
        for (ModelPart.Cube cube : cubes(root)) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
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
