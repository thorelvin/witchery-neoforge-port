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

class NightmareModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSeventeenCubeHierarchyPivotsAnglesAndWindingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = NightmareModel.createBodyLayer().bakeRoot();
        assertEquals(18, root.getAllParts().size());
        assertEquals(17, cubes(root).size());
        assertPose(root.getChild("head"), 0, -6, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, -6, 0, 0, 0, 0);
        assertPose(root.getChild("leg1"), -2, 6, -1, 0, 0, 0);
        assertPose(root.getChild("leg6"), 3, 6, -1, 0, 0, 0);
        assertPose(root.getChild("leg7"), 0, 6, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm").getChild("rf_little"),
                0, 12, 0, -0.148353F, 0, 0.1134464F);
        assertPose(root.getChild("left_arm").getChild("lf_index"),
                0, 12, 0, -0.148353F, 0, 0.1134464F);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(0, mirrored);
    }

    @Test
    void sourceIntegerTickTendrilMotionAndAttackPoseArePreserved() {
        ModelPart root = NightmareModel.createBodyLayer().bakeRoot();
        NightmareModel model = new NightmareModel(root);
        NightmareModel.NightmareRenderState state = new NightmareModel.NightmareRenderState();
        state.entityId = 17;
        state.entityTick = 40;
        state.ageInTicks = 40.75F;
        state.attackTimer = 8.5F;
        model.setupAnim(state);

        float f = 0.01F * (17 % 12);
        assertEquals(-1.8F + Mth.sin(40 * f) * 4.5F * Mth.DEG_TO_RAD,
                root.getChild("leg1").xRot, EPSILON);
        assertEquals(1.839205F + Mth.cos(40 * f) * 2.5F * Mth.DEG_TO_RAD,
                root.getChild("leg1").zRot, EPSILON);
        float triangle = sourceTriangle(8.5F, 15.0F);
        assertEquals(-1.5F + 0.8F * triangle, root.getChild("right_arm").xRot, EPSILON);
        assertEquals(1.5F - 1.5F * triangle, root.getChild("right_arm").zRot, EPSILON);
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

    private static float sourceTriangle(float value, float limit) {
        return (Math.abs(value % limit - limit * 0.5F) - limit * 0.25F) / (limit * 0.25F);
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
