package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GrotesqueModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceHeadPivotSevenCubesAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = GrotesqueModel.createBodyLayer().bakeRoot();
        ModelPart head = root.getChild("head");
        List<ModelPart.Cube> cubes = cubes(head);

        assertEquals(2, root.getAllParts().size());
        assertEquals(7, cubes.size());
        assertPose(head, 0, -9, 0, 0, 0, 0);
        assertCube(cubes.get(0), -4, -8, -4, 4, 0, 4);
        assertCube(cubes.get(1), 4, -12, -0.5F, 5, -4, 0.5F);
        assertCube(cubes.get(2), -5, -12, -0.5F, -4, -4, 0.5F);
        assertCube(cubes.get(3), 1, -4, -5, 2, -2, -4);
        assertCube(cubes.get(4), -2, -2, -6, 2, -1, -4);
        assertCube(cubes.get(5), -1, -6, -6, 1, -3, -4);
        assertCube(cubes.get(6), -2, -4, -5, -1, -2, -4);

        for (ModelPart.Cube cube : cubes) {
            assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void sourceHeadLookUsesNetYawAndPitchDegrees() {
        ModelPart root = GrotesqueModel.createBodyLayer().bakeRoot();
        GrotesqueModel model = new GrotesqueModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.yRot = 37.0F;
        state.xRot = -14.0F;

        model.setupAnim(state);

        assertEquals(37.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);
        assertEquals(-14.0F * Mth.DEG_TO_RAD, root.getChild("head").xRot, EPSILON);
    }

    private static void assertPose(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
    }

    private static void assertCube(ModelPart.Cube cube, float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
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
