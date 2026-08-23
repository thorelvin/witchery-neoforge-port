package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class LegacyBipedModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void exactLegacySevenCubeBipedGeometryAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = LegacyBipedModel.createBodyLayer().bakeRoot();
        LegacyBipedModel<LegacyBipedModel.State> model = new LegacyBipedModel<>(root);
        assertEquals(8, root.getAllParts().size());
        assertEquals(7, cubes(root).size());
        assertPose(root.getChild("head"), 0, 0, 0);
        assertPose(model.hat, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 0, 0);
        assertPose(root.getChild("right_arm"), -5, 2, 0);
        assertPose(root.getChild("left_arm"), 5, 2, 0);
        assertPose(root.getChild("right_leg"), -1.9F, 12, 0);
        assertPose(root.getChild("left_leg"), 1.9F, 12, 0);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(2, mirrored);
    }

    private static void assertPose(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
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
