package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class TreefydModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceElevenCubeGraphPlanesPivotsAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = TreefydModel.createBodyLayer().bakeRoot();
        assertEquals(13, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertPose(root.getChild("head"), 0, 3, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 3, 0, 0, 0, 0);
        assertPose(root.getChild("leaves_v"), 0, 6, 0, 0, Mth.PI / 4, 0);
        assertPose(root.getChild("leaves_h"), 0, 6, 0, 0, Mth.PI / 4, 0);
        assertPose(root.getChild("base"), -3, 19, -3, 0, 0, 0);
        assertPose(root.getChild("leg1"), -2, 20, 4, 0, 0, 0);
        assertPose(root.getChild("leg4"), 2, 20, -4, 0, 0, 0);

        assertEquals(cubes(root.getChild("head").getChild("petals")).get(0).minZ,
                cubes(root.getChild("head").getChild("petals")).get(0).maxZ, EPSILON);
        assertEquals(cubes(root.getChild("head").getChild("tongue")).get(0).minX,
                cubes(root.getChild("head").getChild("tongue")).get(0).maxX, EPSILON);
        assertEquals(cubes(root.getChild("leaves_v")).get(0).minX,
                cubes(root.getChild("leaves_v")).get(0).maxX, EPSILON);

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
    void sourceHeadLookAndPairedFourLegGaitArePreserved() {
        ModelPart root = TreefydModel.createBodyLayer().bakeRoot();
        TreefydModel model = new TreefydModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.walkAnimationPos = 5.0F;
        state.walkAnimationSpeed = 0.7F;
        state.yRot = 21.0F;
        state.xRot = -8.0F;
        model.setupAnim(state);
        float swing = Mth.cos(5.0F * 0.6662F) * 1.4F * 0.7F;
        assertEquals(swing, root.getChild("leg1").xRot, EPSILON);
        assertEquals(swing, root.getChild("leg4").xRot, EPSILON);
        assertEquals(-swing, root.getChild("leg2").xRot, EPSILON);
        assertEquals(-swing, root.getChild("leg3").xRot, EPSILON);
        assertEquals(21.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);
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
