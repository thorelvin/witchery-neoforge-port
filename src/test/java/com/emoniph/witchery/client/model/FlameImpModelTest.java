package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FlameImpModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceBodyGraphAndEffectiveMirrorTimingArePreserved() throws ReflectiveOperationException {
        ModelPart root = FlameImpModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(10, cubes(root).size());
        assertPose(root, "head", 0, 8, 0, 0, 0, 0, 4);
        assertPose(root.getChild("head"), "horn_left", 4, -5, 0,
                0.4089647F, 0, 0.7435722F, 1);
        assertPose(root.getChild("head"), "horn_right", -4, -5, 0,
                0.4089647F, 0, -0.7435722F, 1);
        assertPose(root.getChild("head"), "nose", 0, -4, -3,
                -0.9666439F, 0, 0, 1);
        assertPose(root, "body", 0, 9, 0, 0, 0, 0, 1);
        assertPose(root, "chest", 1, 8, 0, 0, 0, 0, 1);
        assertPose(root, "right_arm", -5, 11, 0, 0, 0, 0, 1);
        assertPose(root, "left_arm", 5, 11, 0, 0, 0, 0, 1);
        assertPose(root, "right_leg", -1.5F, 18, 0, 0, 0, 0, 1);
        assertPose(root, "left_leg", 1.5F, 18, 0, 0, 0, 0, 1);
        for (ModelPart.Cube cube : cubes(root)) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void wingsPreserveTheTextureScaleCapturedBeforeSourceSetTextureSize() throws ReflectiveOperationException {
        ModelPart root = FlameImpWingModel.createBodyLayer().bakeRoot();
        assertEquals(3, root.getAllParts().size());
        assertEquals(2, cubes(root).size());
        assertPose(root, "wing_right", -2, 10, -1,
                0.3047198F, -0.6698132F, -0.6283185F, 1);
        assertPose(root, "wing_left", 2, 10, -1,
                -0.3047198F, 3.811406F, 0.6283185F, 1);
        for (ModelPart.Cube cube : cubes(root)) {
            float[] uv = uvBounds(cube);
            assertEquals(23.0F / 64.0F, uv[0], EPSILON);
            assertEquals(51.0F / 64.0F, uv[1], EPSILON);
            assertEquals(21.0F / 64.0F, uv[2], EPSILON);
            assertEquals(42.0F / 64.0F, uv[3], EPSILON);
        }
    }

    @Test
    void ridingAndAttackPosesResetWithoutLeakingAcrossFrames() {
        ModelPart root = FlameImpModel.createBodyLayer().bakeRoot();
        FlameImpModel model = new FlameImpModel(root);
        FlameImpModel.FlameImpRenderState state = new FlameImpModel.FlameImpRenderState();
        state.riding = true;
        state.attackProgress = 0.6F;
        model.setupAnim(state);
        assertEquals(-Mth.TWO_PI / 5.0F, root.getChild("right_leg").xRot, EPSILON);
        assertEquals(Mth.PI / 10.0F, root.getChild("right_leg").yRot, EPSILON);
        assertEquals(0.1F, root.getChild("right_leg").z, EPSILON);

        state.riding = false;
        state.attackProgress = 0.0F;
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        model.setupAnim(state);
        assertEquals(0.0F, root.getChild("body").yRot, EPSILON);
        assertEquals(-5.0F, root.getChild("right_arm").x, EPSILON);
        assertEquals(0.0F, root.getChild("right_arm").z, EPSILON);
        assertEquals(0.0F, root.getChild("right_arm").yRot, EPSILON);
    }

    private static void assertPose(ModelPart parent, String name, float x, float y, float z,
                                   float xRot, float yRot, float zRot, int cubeCount) {
        ModelPart part = parent.getChild(name);
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
        Object[] polygons = polygons(cube);
        for (Field field : polygons[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons[2]);
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }

    private static float[] uvBounds(ModelPart.Cube cube) throws ReflectiveOperationException {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (Object polygon : polygons(cube)) {
            for (Field field : polygon.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(polygon);
                if (!(value instanceof Object[] vertices)) continue;
                for (Object vertex : vertices) {
                    Field u = vertex.getClass().getDeclaredField("u");
                    Field v = vertex.getClass().getDeclaredField("v");
                    u.setAccessible(true);
                    v.setAccessible(true);
                    float vertexU = u.getFloat(vertex);
                    float vertexV = v.getFloat(vertex);
                    minU = Math.min(minU, vertexU);
                    maxU = Math.max(maxU, vertexU);
                    minV = Math.min(minV, vertexV);
                    maxV = Math.max(maxV, vertexV);
                }
            }
        }
        return new float[]{minU, maxU, minV, maxV};
    }

    private static Object[] polygons(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        return (Object[]) polygonsField.get(cube);
    }
}
