package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class HornedHuntsmanModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixteenPartEighteenCubeGraphAndEffectiveMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = HornedHuntsmanModel.createBodyLayer().bakeRoot();
        assertEquals(17, root.getAllParts().size());
        assertEquals(18, cubes(root).size());

        ModelPart head = root.getChild("head");
        assertPose(head, 0, -16, 0, 0, 0, 0);
        assertPose(head.getChild("horns"), 0, 0, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, -8, 0, 0, 0, 0);
        assertPose(root.getChild("chest"), -10, -16, -5, 0, 0, 0);
        assertPose(root.getChild("hips"), 0, 0, 0, 0, 0, 0);

        ModelPart rightArm = root.getChild("right_arm");
        ModelPart rightForearm = rightArm.getChild("right_forearm");
        ModelPart spear = rightForearm.getChild("spear");
        assertPose(rightArm, -10, -13, 0, 0, 0, 0);
        assertPose(rightForearm, -2, 10, 0, -(float) Math.PI / 6.0F, 0, 0);
        assertPose(spear, 0, 12, 0, 1.5F, 0, 0);
        assertEquals(3, cubes(spear).size());

        ModelPart rightLeg = root.getChild("right_leg");
        ModelPart rightShin = rightLeg.getChild("right_shin");
        assertPose(rightLeg, -4, 3, -1, (float) Math.PI / 6.0F, 0, 0);
        assertPose(rightShin, 0, 10, 0, -0.9F, 0, 0);
        assertPose(rightShin.getChild("right_foot"), 0, 10, 0, 0.5F, 0, 0);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(3, mirrored);
    }

    @Test
    void sourceGaitAndAttackTimerPoseArePreserved() {
        ModelPart root = HornedHuntsmanModel.createBodyLayer().bakeRoot();
        HornedHuntsmanModel model = new HornedHuntsmanModel(root);
        HornedHuntsmanModel.HuntsmanRenderState state = new HornedHuntsmanModel.HuntsmanRenderState();
        state.walkAnimationPos = 4.0F;
        state.walkAnimationSpeed = 0.7F;
        state.attackTimer = 8;
        state.partialTick = 0.25F;
        model.setupAnim(state);

        float wave = HornedHuntsmanModel.triangleWave(4.0F, 13.0F) * 0.7F;
        float leftLeg = -1.3F * wave + 0.5F;
        float rightLeg = 1.3F * wave + 0.5F;
        assertEquals(leftLeg, root.getChild("left_leg").xRot, EPSILON);
        assertEquals(rightLeg, root.getChild("right_leg").xRot, EPSILON);
        assertEquals(0.8F * (rightLeg - 0.5F) - 1.0F,
                root.getChild("left_leg").getChild("left_shin").xRot, EPSILON);
        assertEquals(Math.max(1.4F * (leftLeg - 0.5F) + 0.5F, 0.2F),
                root.getChild("left_leg").getChild("left_shin").getChild("left_foot").xRot,
                EPSILON);
        assertEquals(-2.0F + 1.5F * HornedHuntsmanModel.triangleWave(7.75F, 10.0F),
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
