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

class ToadModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixPartTenCubeGraphExactEyesAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = ToadModel.createBodyLayer().bakeRoot();
        assertEquals(7, root.getAllParts().size());
        assertEquals(10, cubes(root).size());
        List<ModelPart.Cube> headCubes = cubes(root.getChild("head"));
        assertEquals(-2.5F, headCubes.get(1).minX, EPSILON);
        assertEquals(0.5F, headCubes.get(2).minX, EPSILON);
        assertPose(root.getChild("body"), 0, 20, -1, -0.4833219F, 0, 0);
        assertPose(root.getChild("arm_right"), -2, 20, -1, -0.3346075F, 0, 0);
        assertPose(root.getChild("arm_left"), 2, 20, -1, -0.3346075F, 0, 0);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(10, mirrored);
    }

    @Test
    void walkingBiasArmGaitAndSittingLegsMatchSource() {
        ModelPart root = ToadModel.createBodyLayer().bakeRoot();
        ToadModel model = new ToadModel(root);
        ToadModel.ToadRenderState state = new ToadModel.ToadRenderState();
        state.walkAnimationPos = 2.1F;
        state.walkAnimationSpeed = 0.6F;
        model.setupAnim(state);

        float rightArm = Mth.cos(2.1F * 0.6662F) * 1.4F * 0.6F;
        float leftArm = Mth.cos(2.1F * 0.6662F + Mth.PI) * 1.4F * 0.6F;
        assertEquals(rightArm, root.getChild("arm_right").xRot, EPSILON);
        assertEquals(leftArm, root.getChild("arm_left").xRot, EPSILON);
        assertEquals(rightArm + 0.5F, root.getChild("leg_right").xRot, EPSILON);
        assertEquals(rightArm + 0.5F, root.getChild("leg_left").xRot, EPSILON);

        state.walkAnimationSpeed = 0.05F;
        model.setupAnim(state);
        assertEquals(Mth.cos(2.1F * 0.6662F) * 1.4F * 0.05F,
                root.getChild("leg_right").xRot, EPSILON);

        state.sitting = true;
        model.setupAnim(state);
        assertEquals(-Mth.PI / 8.0F, root.getChild("leg_right").xRot, EPSILON);
        assertEquals(-Mth.PI / 8.0F, root.getChild("leg_left").xRot, EPSILON);
        assertEquals(-0.3346075F, root.getChild("arm_right").xRot, EPSILON);
    }

    @Test
    void babyLayerUsesSourceSplitHeadAndBodyTransforms() {
        ModelPart root = ToadModel.createBabyLayer().bakeRoot();
        ToadModel model = new ToadModel(root);
        ToadModel.ToadRenderState state = new ToadModel.ToadRenderState();
        state.isBaby = true;
        model.setupAnim(state);

        ModelPart head = root.getChild("head");
        assertEquals(0.75F, head.xScale, EPSILON);
        assertEquals(22.5F, head.y, EPSILON);
        assertEquals(-0.75F, head.z, EPSILON);
        ModelPart body = root.getChild("body");
        assertEquals(0.5F, body.xScale, EPSILON);
        assertEquals(22.0F, body.y, EPSILON);
        assertEquals(-0.5F, body.z, EPSILON);
        assertEquals(23.5F, root.getChild("leg_right").y, EPSILON);
        assertEquals(1.5F, root.getChild("leg_right").z, EPSILON);
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
