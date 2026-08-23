package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class HobgoblinModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixPartThirteenCubeGraphFaceAndMirrorsArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = HobgoblinModel.createBodyLayer().bakeRoot();
        assertEquals(8, root.getAllParts().size());
        assertEquals(14, cubes(root).size());
        assertPose(root.getChild("head"), 0, 11, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 11, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -5, 12, 0, 0, 0, 0);
        assertPose(root.getChild("right_leg"), -2, 18, 0, 0, 0, 0);
        List<ModelPart.Cube> head = cubes(root.getChild("head"));
        assertCube(head.get(1), -0.5F, -6, -5, 0.5F, -3, -4);
        assertCube(head.get(3), -0.5F, -4, -7, 0.5F, -2, -5);
        assertCube(head.get(4), 6, -7, 0, 8, -5, 1);
        assertCube(head.get(7), -8, -7, 0, -6, -5, 1);

        HobgoblinModel model = new HobgoblinModel(root);
        assertFalse(root.getChild("head").getChild("hat").visible);
        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(13, mirrored);
    }

    @Test
    void workingAndWorshippingBranchesMatchSourceRuntimePivots() {
        ModelPart root = HobgoblinModel.createBodyLayer().bakeRoot();
        HobgoblinModel model = new HobgoblinModel(root);
        HobgoblinModel.HobgoblinRenderState state = new HobgoblinModel.HobgoblinRenderState();
        state.holdingRightItem = true;
        state.working = true;
        state.workingWithKoboldite = true;
        state.entityTick = 5;
        model.setupAnim(state);
        float pitchBob = Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        assertEquals(-Mth.PI / 10.0F - 5 * 0.3F + pitchBob,
                root.getChild("right_arm").xRot, EPSILON);
        assertEquals(11.0F, root.getChild("head").y, EPSILON);
        assertEquals(11.0F, root.getChild("body").y, EPSILON);
        assertEquals(0.1F, root.getChild("right_leg").z, EPSILON);

        state.working = false;
        state.worshipping = true;
        model.setupAnim(state);
        assertEquals(0.5F, root.getChild("body").xRot, EPSILON);
        assertEquals(0.5F, root.getChild("head").xRot, EPSILON);
        assertEquals(13.0F, root.getChild("head").y, EPSILON);
        assertEquals(13.0F, root.getChild("body").y, EPSILON);
        assertEquals(3.0F, root.getChild("right_leg").z, EPSILON);
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
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
