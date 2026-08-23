package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.renderer.WolfmanRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WolfmanModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceTenPartThirteenCubeGraphAndRootTailArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = WolfmanModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(13, cubes(root).size());
        assertPose(root.getChild("head_main"), 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("body_upper"), 0.0F, -0.1F, -2.0F,
                0.4098033F, 0.0F, 0.0F);
        assertPose(root.getChild("body_upper").getChild("body_lower"),
                0.0F, 5.0F, -1.5F, 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("tail"), 0.0F, 11.9F, 3.6F,
                0.59184116F, 0.0F, 0.0F);
        assertPose(root.getChild("leg_left_upper"), 2.0F, 12.0F, 0.0F,
                -0.4098033F, 0.0F, 0.0F);
        assertPose(root.getChild("leg_right_upper"), -2.0F, 12.0F, 0.0F,
                -0.4098033F, 0.0F, 0.0F);
        assertPose(root.getChild("arm_left"), 6.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        assertPose(root.getChild("arm_right"), -5.8F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(2, mirrored);
    }

    @Test
    void sourceAttackGaitTailAndRuntimeArmPivotsArePreserved() {
        ModelPart root = WolfmanModel.createBodyLayer().bakeRoot();
        WolfmanModel model = new WolfmanModel(root);
        WolfmanRenderer.WolfmanRenderState state = new WolfmanRenderer.WolfmanRenderState();
        state.walkAnimationPos = 2.0F;
        state.walkAnimationSpeed = 0.6F;
        state.ageInTicks = 10.0F;
        state.attackTimer = 8.5F;
        state.attackProgress = 0.0F;
        model.setupAnim(state);

        float triangle = WolfmanModel.triangleWave(8.5F, 10.0F);
        float idlePitch = Mth.sin(10.0F * 0.067F) * 0.05F;
        float idleRoll = Mth.cos(10.0F * 0.09F) * 0.05F + 0.05F;
        assertEquals(-2.0F + 1.5F * triangle + idlePitch,
                root.getChild("arm_right").xRot, EPSILON);
        assertEquals(-1.0F + 0.9F * triangle - idlePitch,
                root.getChild("arm_left").xRot, EPSILON);
        assertEquals(idleRoll, root.getChild("arm_right").zRot, EPSILON);
        assertEquals(-idleRoll, root.getChild("arm_left").zRot, EPSILON);
        assertEquals(-5.0F, root.getChild("arm_right").x, EPSILON);
        assertEquals(5.0F, root.getChild("arm_left").x, EPSILON);

        assertEquals(Math.max(-0.4098033F
                        + Mth.cos(2.0F * 0.6662F) * 1.4F * 0.6F, -0.8F),
                root.getChild("leg_right_upper").xRot, EPSILON);
        assertEquals(Math.max(-0.4098033F
                        + Mth.cos(2.0F * 0.6662F + Mth.PI) * 1.4F * 0.6F, -0.8F),
                root.getChild("leg_left_upper").xRot, EPSILON);
        assertEquals(0.59184116F + 0.5F, root.getChild("tail").xRot, EPSILON);
        assertEquals(0.25F * Mth.cos(0.9F) + 0.05F,
                root.getChild("tail").zRot, EPSILON);
        assertEquals(0.0F,
                root.getChild("leg_right_upper").getChild("leg_right_lower").xRot, EPSILON);
    }

    @Test
    void sourceSittingCrouchingAndBowBranchesArePreserved() {
        ModelPart root = WolfmanModel.createBodyLayer().bakeRoot();
        WolfmanModel model = new WolfmanModel(root);
        WolfmanRenderer.WolfmanRenderState state = new WolfmanRenderer.WolfmanRenderState();
        state.xRot = 10.0F;
        state.yRot = 20.0F;
        state.attackProgress = 0.0F;
        state.sourceSitting = true;
        state.sourceCrouching = true;
        state.aimedBow = true;
        model.setupAnim(state);

        assertEquals(-Mth.PI * 2.0F / 5.0F,
                root.getChild("leg_right_upper").xRot, EPSILON);
        assertEquals(Mth.PI / 10.0F, root.getChild("leg_right_upper").yRot, EPSILON);
        assertEquals(9.0F, root.getChild("leg_right_upper").y, EPSILON);
        assertEquals(4.0F, root.getChild("leg_right_upper").z, EPSILON);
        assertEquals(0.5F, root.getChild("body_upper").xRot, EPSILON);
        assertEquals(-Mth.HALF_PI + 10.0F * Mth.DEG_TO_RAD,
                root.getChild("arm_right").xRot, EPSILON);
        assertEquals(-0.1F + 20.0F * Mth.DEG_TO_RAD,
                root.getChild("arm_right").yRot, EPSILON);
        assertEquals(0.1F, root.getChild("arm_right").zRot, EPSILON);
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
