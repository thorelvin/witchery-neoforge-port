package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class LilithModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceTwentyFourPartGraphFlatPlanesAndMirrorsArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = LilithModel.createBodyLayer().bakeRoot();
        assertEquals(25, root.getAllParts().size());
        assertEquals(24, cubes(root).size());
        assertPose(root.getChild("neck"), 0, -13, 0, 0, 0, 0);
        assertPose(root.getChild("body_chest"), 0, -9.8F, -1.9F, (float) Math.PI / 4, 0, 0);

        ModelPart rightWing = root.getChild("arm_right").getChild("arm_right_wing");
        ModelPart leftWing = root.getChild("arm_left").getChild("arm_left_wing");
        ModelPart rightHorn = root.getChild("head").getChild("horn_right");
        ModelPart leftHorn = root.getChild("head").getChild("horn_left");
        assertEquals(cubes(rightWing).get(0).minX, cubes(rightWing).get(0).maxX, EPSILON);
        assertEquals(cubes(leftWing).get(0).minX, cubes(leftWing).get(0).maxX, EPSILON);
        assertEquals(cubes(rightHorn).get(0).minZ, cubes(rightHorn).get(0).maxZ, EPSILON);
        assertEquals(cubes(leftHorn).get(0).minZ, cubes(leftHorn).get(0).maxZ, EPSILON);
        assertPose(root.getChild("head").getChild("head2").getChild("head3"),
                0, 0, 0, 0, 0, 0);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(7, mirrored);
    }

    @Test
    void sourceGaitRidingAndYawCoupledAttackPoseArePreserved() {
        ModelPart root = LilithModel.createBodyLayer().bakeRoot();
        LilithModel model = new LilithModel(root);
        LilithModel.LilithRenderState state = new LilithModel.LilithRenderState();
        state.walkAnimationPos = 3.0F;
        state.walkAnimationSpeed = 0.6F;
        state.ageInTicks = 12.0F;
        state.riding = true;
        model.setupAnim(state);
        assertEquals((float) (-Math.PI * 2.0 / 5.0), root.getChild("leg_right").xRot, EPSILON);
        assertEquals((float) (Math.PI / 10.0), root.getChild("leg_right").yRot, EPSILON);
        assertEquals((float) (-Math.PI / 10.0), root.getChild("leg_left").yRot, EPSILON);

        state.riding = false;
        state.attackTimer = 9;
        state.yRot = 2.5F;
        model.setupAnim(state);
        assertEquals(-2.0F + 1.5F * sourceTriangle(6.5F, 10.0F),
                root.getChild("arm_right").xRot, EPSILON);
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
