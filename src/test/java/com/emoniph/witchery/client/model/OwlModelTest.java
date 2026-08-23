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

class OwlModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixPartNineCubeGraphAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = OwlModel.createBodyLayer().bakeRoot();
        assertEquals(7, root.getAllParts().size());
        assertEquals(9, cubes(root).size());
        assertPose(root.getChild("head"), 0, 15, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 15, 0, 0, 0, 0);
        assertPose(root.getChild("right_arm"), -3, 16, 0, 0, 0, 0);
        assertPose(root.getChild("left_arm"), 3, 16, 0, 0, 0, 0);
        assertPose(root.getChild("body").getChild("right_leg"), -2, 8, -1, 0, 0, 0);
        assertPose(root.getChild("body").getChild("left_leg"), 2, 8, -1, 0, 0, 0);

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
    void landedFlyingAndSittingPosesMatchSource() {
        ModelPart root = OwlModel.createBodyLayer().bakeRoot();
        OwlModel model = new OwlModel(root);
        OwlModel.OwlRenderState state = new OwlModel.OwlRenderState();
        state.ageInTicks = 18.0F;
        state.yRot = 27.0F;
        state.xRot = -11.0F;
        state.landed = true;
        model.setupAnim(state);
        assertEquals(0.0F, root.getChild("body").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("right_arm").zRot, EPSILON);
        assertEquals(0.1F, root.getChild("body").getChild("right_leg").yRot, EPSILON);
        assertEquals(27.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);
        assertEquals(-11.0F * Mth.DEG_TO_RAD, root.getChild("head").xRot, EPSILON);

        state.landed = false;
        state.sitting = true;
        model.setupAnim(state);
        float tilt = Mth.PI / 4.0F + Mth.cos(1.8F) * 0.15F;
        float wing = Mth.cos(9.0F) * Mth.PI * 0.4F + 1.4F;
        assertEquals(tilt, root.getChild("body").xRot, EPSILON);
        assertEquals(tilt, root.getChild("body").getChild("right_leg").xRot, EPSILON);
        assertEquals(wing, root.getChild("right_arm").zRot, EPSILON);
        assertEquals(-wing, root.getChild("left_arm").zRot, EPSILON);
        assertEquals(0.5F, root.getChild("body").getChild("right_leg").yRot, EPSILON);
        assertEquals(-0.5F, root.getChild("body").getChild("left_leg").yRot, EPSILON);
    }

    @Test
    void babyLayerUsesSourceHeadAndBodyGroups() {
        ModelPart root = OwlModel.createBabyLayer().bakeRoot();
        OwlModel model = new OwlModel(root);
        OwlModel.OwlRenderState state = new OwlModel.OwlRenderState();
        state.isBaby = true;
        state.landed = true;
        model.setupAnim(state);

        ModelPart head = root.getChild("head");
        assertEquals(0.75F, head.xScale, EPSILON);
        assertEquals(19.5F, head.y, EPSILON);
        ModelPart body = root.getChild("body");
        assertEquals(0.5F, body.xScale, EPSILON);
        assertEquals(19.5F, body.y, EPSILON);
        assertEquals(0.5F, root.getChild("right_arm").xScale, EPSILON);
        assertEquals(20.0F, root.getChild("right_arm").y, EPSILON);
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
