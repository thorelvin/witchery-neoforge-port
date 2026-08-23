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

class LeonardModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixteenPartHierarchyAndEffectiveMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = LeonardModel.createBodyLayer().bakeRoot();
        assertEquals(17, root.getAllParts().size());
        assertEquals(16, cubes(root).size());
        ModelPart neck = root.getChild("neck");
        ModelPart head = neck.getChild("head");
        assertPose(neck, 0, 0, 0, 0.1745329F, 0, 0, 9);
        assertPose(head, 0, 0, 0, 0.1745329F, 0, 0, 8);
        assertPose(head.getChild("ear_left"), 0, 0, 0,
                -0.5129616F, -Mth.PI / 12.0F, -1.180008F, 1);
        assertPose(head.getChild("ear_right"), 0, 0, 0,
                -0.3346075F, 0.0371786F, 1.226894F, 1);
        assertPose(root.getChild("right_leg").getChild("gown_lower_right"),
                2, -12, 0, 0, 0, 0, 1);
        assertPose(root.getChild("left_leg").getChild("gown_lower_left"),
                -2, -12, 0, 0, 0, 0, 1);
        for (ModelPart.Cube cube : cubes(root)) {
            assertEquals(-1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void completeAttackRidingAndIdlePoseResetsAcrossFrames() {
        ModelPart root = LeonardModel.createBodyLayer().bakeRoot();
        LeonardModel model = new LeonardModel(root);
        LeonardModel.LeonardRenderState state = new LeonardModel.LeonardRenderState();
        state.riding = true;
        state.attackProgress = 0.45F;
        state.attackTimer = 7.25F;
        state.xRot = -10.0F;
        model.setupAnim(state);

        float expectedSpecialAttack = -2.0F
                + 1.5F * (Math.abs(7.25F % 10.0F - 5.0F) - 2.5F) / 2.5F;
        assertEquals(expectedSpecialAttack, root.getChild("right_arm").xRot, EPSILON);
        assertEquals(-Mth.TWO_PI / 5.0F, root.getChild("right_leg").xRot, EPSILON);
        assertEquals(Mth.PI / 10.0F, root.getChild("right_leg").yRot, EPSILON);
        assertEquals(0.1F, root.getChild("right_leg").z, EPSILON);

        state.riding = false;
        state.attackProgress = 0.0F;
        state.attackTimer = 0.0F;
        state.xRot = 0.0F;
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.ageInTicks = 0.0F;
        model.setupAnim(state);
        assertEquals(0.0F, root.getChild("body").yRot, EPSILON);
        assertEquals(-5.0F, root.getChild("right_arm").x, EPSILON);
        assertEquals(0.0F, root.getChild("right_arm").z, EPSILON);
        assertEquals(0.0F, root.getChild("right_arm").yRot, EPSILON);
    }

    private static void assertPose(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot, int cubeCount) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
        assertEquals(cubeCount, cubes(part).size());
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
