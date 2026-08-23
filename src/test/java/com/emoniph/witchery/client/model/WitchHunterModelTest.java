package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.renderer.WitchHunterRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WitchHunterModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceBipedAndThreeUnmirroredHunterCubesArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = WitchHunterModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(10, cubes(root).size());
        assertPose(root.getChild("head"), 0, 0, 0, 0, 0, 0);
        assertPose(root.getChild("head").getChild("hat_brim"), 0, -6, 0, 0, 0, 0);
        assertPose(root.getChild("head").getChild("hat_top"), 0, -10, 0, 0, 0, 0);
        assertPose(root.getChild("body").getChild("coat_bottom"), 0, 11, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -5, 2, 0, 0, 0, 0);
        assertPose(root.getChild("left_arm"), 5, 2, 0, 0, 0, 0);
        assertPose(root.getChild("right_leg"), -1.9F, 12, 0, 0, 0, 0);
        assertPose(root.getChild("left_leg"), 1.9F, 12, 0, 0, 0, 0);

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
    void sourceGaitSwingCrouchAndSingleHeadwearTransformArePreserved() {
        ModelPart root = WitchHunterModel.createBodyLayer().bakeRoot();
        WitchHunterModel model = new WitchHunterModel(root);
        WitchHunterRenderer.WitchHunterRenderState state =
                new WitchHunterRenderer.WitchHunterRenderState();
        state.walkAnimationPos = 3.0F;
        state.walkAnimationSpeed = 0.6F;
        state.ageInTicks = 9.0F;
        state.xRot = 15.0F;
        state.yRot = -20.0F;
        state.swingProgress = 0.0F;
        state.sourceCrouching = true;
        model.setupAnim(state);

        float pitchBob = Mth.sin(9.0F * 0.067F) * 0.05F;
        assertEquals(Mth.cos(3.0F * 0.6662F + Mth.PI) * 0.6F + 0.4F + pitchBob,
                root.getChild("right_arm").xRot, EPSILON);
        assertEquals(Mth.cos(3.0F * 0.6662F) * 1.4F * 0.6F,
                root.getChild("right_leg").xRot, EPSILON);
        assertEquals(9.0F, root.getChild("right_leg").y, EPSILON);
        assertEquals(4.0F, root.getChild("right_leg").z, EPSILON);
        assertEquals(0.5F, root.getChild("body").xRot, EPSILON);
        assertEquals(1.0F, root.getChild("head").y, EPSILON);
        assertEquals(0.0F, model.hat.y, EPSILON);
        assertEquals(0.0F, model.hat.xRot, EPSILON);
        assertEquals(0.0F, model.hat.yRot, EPSILON);
    }

    @Test
    void sourceRidingHeldItemAndBowBranchesArePreserved() {
        ModelPart root = WitchHunterModel.createBodyLayer().bakeRoot();
        WitchHunterModel model = new WitchHunterModel(root);
        WitchHunterRenderer.WitchHunterRenderState state =
                new WitchHunterRenderer.WitchHunterRenderState();
        state.ageInTicks = 0.0F;
        state.xRot = 10.0F;
        state.yRot = 20.0F;
        state.swingProgress = 0.0F;
        state.sourceRiding = true;
        state.heldItemRight = 1;
        state.aimedBow = true;
        model.setupAnim(state);

        assertEquals(-Mth.PI * 2.0F / 5.0F, root.getChild("right_leg").xRot, EPSILON);
        assertEquals(Mth.PI / 10.0F, root.getChild("right_leg").yRot, EPSILON);
        assertEquals(-Mth.HALF_PI + 10.0F * Mth.DEG_TO_RAD,
                root.getChild("right_arm").xRot, EPSILON);
        assertEquals(-0.1F + 20.0F * Mth.DEG_TO_RAD,
                root.getChild("right_arm").yRot, EPSILON);
        assertEquals(0.1F, root.getChild("right_arm").zRot, EPSILON);
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
