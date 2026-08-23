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

class MandrakeEntityModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceEightCubesPivotsFlatLeavesAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = MandrakeEntityModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(8, cubes(root).size());
        assertPose(root.getChild("head"), 0, 16, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 16, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -2, 17, 0, 0, 0, 1.047198F);
        assertPose(root.getChild("left_arm"), 2, 17, 0, 0, 0, -1.047198F);
        assertPose(root.getChild("right_leg"), -1, 21, 0, 0, 0, 0);
        assertPose(root.getChild("left_leg"), 1, 21, 0, 0, 0, 0);

        ModelPart.Cube leaves = cubes(root.getChild("head").getChild("leaves")).get(0);
        assertEquals(leaves.minZ, leaves.maxZ, EPSILON);
        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(4, mirrored);
    }

    @Test
    void sourceArmsUseTriangleWaveWhileHeadAndLegsRemainFixed() {
        ModelPart root = MandrakeEntityModel.createBodyLayer().bakeRoot();
        MandrakeEntityModel model = new MandrakeEntityModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.walkAnimationPos = 5.0F;
        state.walkAnimationSpeed = 0.6F;
        state.yRot = 30.0F;
        state.xRot = 20.0F;
        model.setupAnim(state);
        float wave = MandrakeEntityModel.triangleWave(5.0F, 13.0F);
        assertEquals((-0.2F + 1.5F * wave) * 0.6F, root.getChild("right_arm").xRot, EPSILON);
        assertEquals((-0.2F - 1.5F * wave) * 0.6F, root.getChild("left_arm").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("head").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("head").yRot, EPSILON);
        assertEquals(0.0F, root.getChild("right_leg").xRot, EPSILON);
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
