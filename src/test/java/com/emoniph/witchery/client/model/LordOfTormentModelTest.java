package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class LordOfTormentModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceTwelvePartTwentyCubeGraphAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = LordOfTormentModel.createBodyLayer().bakeRoot();
        assertEquals(13, root.getAllParts().size());
        assertEquals(20, cubes(root).size());
        ModelPart head = root.getChild("head");
        assertEquals(11, cubes(head).size());
        assertPose(head, 0, -6, 0, 0, 0, 0);
        assertPose(head.getChild("horn_right"), 0, 0, 0, 0.5948578F, 0, -0.1858931F);
        assertPose(head.getChild("horn_left"), 0, 0, 0, 0.5948578F, 0, 0.1858931F);
        assertPose(root.getChild("wings_left"), 0, 1, 5, 0, 0, 0);
        assertPose(root.getChild("wings_right"), 0, 1, 5, 0, 0, 0);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(9, mirrored);
    }

    @Test
    void wingAndAttackPosesMatchSourceAtRepresentativeFrames() {
        ModelPart root = LordOfTormentModel.createBodyLayer().bakeRoot();
        LordOfTormentModel model = new LordOfTormentModel(root);
        LordOfTormentModel.LordOfTormentRenderState state = new LordOfTormentModel.LordOfTormentRenderState();
        state.moving = true;
        state.attackTimer = 0.0F;
        model.setupAnim(state);
        assertEquals(0.4F, root.getChild("wings_left").yRot, EPSILON);
        assertEquals(-0.4F, root.getChild("wings_right").yRot, EPSILON);

        state.moving = false;
        state.walkAnimationSpeed = 0.6F;
        state.ageInTicks = 12.0F;
        state.attackTimer = 8.5F;
        model.setupAnim(state);
        float triangle = (Math.abs(8.5F % 15.0F - 7.5F) - 3.75F) / 3.75F;
        assertEquals(-1.5F + 0.8F * triangle, root.getChild("right_arm1").xRot, EPSILON);
        assertEquals(1.5F - 1.5F * triangle, root.getChild("right_arm1").zRot, EPSILON);
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
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
