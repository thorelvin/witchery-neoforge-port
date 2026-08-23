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

class FlyingMonkeyModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceFourteenPartCubeGraphGeometryHierarchyAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = FlyingMonkeyModel.createBodyLayer().bakeRoot();
        assertEquals(15, root.getAllParts().size());
        assertEquals(14, cubes(root).size());
        assertPose(root.getChild("tail"), 0.0F, 18.5F, 5.3F, -0.63739425F, 0, 0);
        assertPose(root.getChild("rightArm"), -3.5F, 14, 0, -0.18203785F, 0, 0);
        assertPose(root.getChild("leftArm"), 4, 14, 0, -0.18203785F, 0, 0);
        assertPose(root.getChild("shoulder"), 0, 16, 1, Mth.HALF_PI, 0, 0);
        assertPose(root.getChild("body"), 0, 15.8F, 2, 0.59184116F, 0, 0);
        assertPose(root.getChild("rightWing"), -1, 14, 2.5F,
                -0.68294734F, 0.3642502F, 0.5462881F);
        assertPose(root.getChild("leftWing"), 1, 14, 2.5F,
                -0.68294734F, -0.3642502F, -0.5462881F);
        assertPose(root.getChild("head"), 0, 12, -1.5F, 0, 0, 0);
        assertEquals(2.5F,
                cubes(root.getChild("head").getChild("headFace")).getFirst().maxX, EPSILON);

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
    void landedFlyingSittingTailLookAndAttackPosesMatchSource() {
        ModelPart root = FlyingMonkeyModel.createBodyLayer().bakeRoot();
        FlyingMonkeyModel model = new FlyingMonkeyModel(root);
        FlyingMonkeyModel.FlyingMonkeyRenderState state = new FlyingMonkeyModel.FlyingMonkeyRenderState();
        state.landed = false;
        state.ageInTicks = 7.0F;
        state.walkAnimationSpeed = 0.4F;
        state.yRot = 18.0F;
        state.xRot = -11.0F;
        model.setupAnim(state);

        float wingZ = Mth.cos(7.0F * 0.5F) * Mth.PI * 0.4F + 0.2F;
        float armSwing = Mth.sin(7.0F * 0.067F) * 0.05F;
        assertEquals(wingZ, root.getChild("rightWing").zRot, EPSILON);
        assertEquals(-wingZ, root.getChild("leftWing").zRot, EPSILON);
        assertEquals(0.3642502F, root.getChild("rightWing").yRot, EPSILON);
        assertEquals(12.0F, root.getChild("rightWing").y, EPSILON);
        assertEquals(0.2F + armSwing, root.getChild("rightArm").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("rightLeg").xRot, EPSILON);
        assertEquals(-0.63739425F - 0.4F - 0.1F, root.getChild("tail").xRot, EPSILON);
        assertEquals(18.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);
        assertEquals(-11.0F * Mth.DEG_TO_RAD, root.getChild("head").xRot, EPSILON);

        state.landed = true;
        state.sitting = true;
        state.walkAnimationSpeed = 0.0F;
        model.setupAnim(state);
        assertPose(root.getChild("rightWing"), -1, 14, 2.5F,
                -0.68294734F, 0.3642502F, 0.5462881F);
        assertEquals(-1.3F, root.getChild("rightLeg").xRot, EPSILON);
        assertEquals(21.0F, root.getChild("rightLeg").y, EPSILON);
        assertEquals(2.0F, root.getChild("rightLeg").z, EPSILON);
        assertEquals(0.1F, root.getChild("body").xRot, EPSILON);
        assertPose(root.getChild("tail"), 0, 20, 4, -0.9F, 0,
                3.0F * Mth.cos(7.0F * 0.09F) * 0.05F + 0.05F);

        state.attackTimer = 8;
        model.setupAnim(state);
        assertEquals(FlyingMonkeyModel.sourceAttackPitch(8, 18.0F),
                root.getChild("rightArm").xRot, EPSILON);
    }

    @Test
    void attackPitchPreservesSourceUseOfNetHeadYawInTriangleWave() {
        float triangle = (Math.abs((8.0F - 18.0F) % 10.0F - 5.0F) - 2.5F) / 2.5F;
        assertEquals(-2.0F + 1.5F * triangle,
                FlyingMonkeyModel.sourceAttackPitch(8, 18.0F), EPSILON);
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
