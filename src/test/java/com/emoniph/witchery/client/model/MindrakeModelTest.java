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

class MindrakeModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceFiveCubeRigidGraphFlatLeavesAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = MindrakeModel.createBodyLayer().bakeRoot();
        assertEquals(6, root.getAllParts().size());
        assertEquals(5, cubes(root).size());
        assertPose(root.getChild("leaves"), 0, 13, 0);
        assertPose(root.getChild("body_top"), 0, 19, 0);
        assertPose(root.getChild("body_bottom"), 0, 20, 0);
        assertPose(root.getChild("leg_left"), 1, 23, 0);
        assertPose(root.getChild("leg_right"), -1, 23, 0);
        ModelPart.Cube leaves = cubes(root.getChild("leaves")).get(0);
        assertEquals(leaves.minZ, leaves.maxZ, EPSILON);

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
    void sourceModelHasNoInventedLegOrLeafAnimation() {
        ModelPart root = MindrakeModel.createBodyLayer().bakeRoot();
        MindrakeModel model = new MindrakeModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.walkAnimationPos = 8.0F;
        state.walkAnimationSpeed = 1.0F;
        state.ageInTicks = 100.0F;
        state.yRot = 40.0F;
        model.setupAnim(state);
        assertEquals(0.0F, root.getChild("leg_left").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("leg_right").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("leaves").yRot, EPSILON);
        assertEquals(0.0F, root.getChild("leaves").zRot, EPSILON);
    }

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(0.0F, part.xRot, EPSILON);
        assertEquals(0.0F, part.yRot, EPSILON);
        assertEquals(0.0F, part.zRot, EPSILON);
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
