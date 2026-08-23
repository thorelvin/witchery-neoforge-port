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

class GoblinGulgModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSevenPartElevenCubeGraphFaceAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = GoblinGulgModel.createBodyLayer().bakeRoot();
        assertEquals(8, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertPose(root.getChild("body"), 0, 8, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -6, 2, 0, 0, 0, 0);
        assertPose(root.getChild("left_arm"), 5, 2, 0, 0, 0, 0);
        List<ModelPart.Cube> head = cubes(root.getChild("head"));
        assertCube(head.get(1), -2, -4, -5, -1, -2, -4);
        assertCube(head.get(2), 1, -4, -5, 2, -2, -4);
        assertCube(head.get(3), -1, -6, -6, 1, -3, -4);
        assertCube(head.get(4), -2, -2, -6, 2, -1, -4);
        assertCube(cubes(root.getChild("left_arm")).getFirst(), 0, -2, -2, 4, 14, 2);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(11, mirrored);
    }

    @Test
    void sourceSwingIdleCrouchHeldAndAttackBranchesPreserveFinalRuntimePivots() {
        ModelPart root = GoblinGulgModel.createBodyLayer().bakeRoot();
        GoblinGulgModel model = new GoblinGulgModel(root);
        GoblinGulgModel.GulgRenderState state = new GoblinGulgModel.GulgRenderState();
        state.walkAnimationPos = 2.0F;
        state.walkAnimationSpeed = 0.6F;
        state.ageInTicks = 11.0F;
        model.setupAnim(state);
        assertEquals(-5.0F, root.getChild("right_arm").x, EPSILON);
        assertEquals(5.0F, root.getChild("left_arm").x, EPSILON);
        assertEquals(0.1F, root.getChild("right_leg").z, EPSILON);
        assertEquals(12.0F, root.getChild("right_leg").y, EPSILON);
        float rightBase = Mth.cos(2.0F * 0.6662F + Mth.PI) * 0.6F;
        assertEquals(rightBase + Mth.sin(11.0F * 0.067F) * 0.05F,
                root.getChild("right_arm").xRot, EPSILON);

        state.crouching = true;
        state.holdingRightItem = true;
        model.setupAnim(state);
        assertEquals(0.5F, root.getChild("body").xRot, EPSILON);
        assertEquals(9.0F, root.getChild("right_leg").y, EPSILON);
        assertEquals(4.0F, root.getChild("right_leg").z, EPSILON);
        assertEquals(1.0F, root.getChild("head").y, EPSILON);

        state.attackTimer = 8;
        state.yRot = 1.5F;
        model.setupAnim(state);
        assertEquals(-2.0F + 1.5F * GoblinGulgModel.triangleWave(6.5F, 10.0F),
                root.getChild("right_arm").xRot, EPSILON);
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
