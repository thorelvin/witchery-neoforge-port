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

class ParasyticLouseModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSevenSegmentsThreeFinsPivotsAndWindingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = ParasyticLouseModel.createBodyLayer().bakeRoot();
        assertEquals(11, root.getAllParts().size());
        assertEquals(10, cubes(root).size());
        float[] z = {-3.5F, -1.5F, 0.5F, 3.0F, 6.0F, 8.5F, 10.5F};
        int[] heights = {2, 3, 3, 3, 2, 1, 1};
        for (int i = 0; i < 7; i++) {
            assertPose(root.getChild("body_" + i), 0, 24 - heights[i], z[i]);
        }
        assertPose(root.getChild("fin_0"), 0, 16, z[2]);
        assertPose(root.getChild("fin_1"), 0, 20, z[4]);
        assertPose(root.getChild("fin_2"), 0, 19, z[1]);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(0, mirrored);
    }

    @Test
    void sourceAgeWaveMovesSegmentPivotsAndCopiesThemToFins() {
        ModelPart root = ParasyticLouseModel.createBodyLayer().bakeRoot();
        ParasyticLouseModel model = new ParasyticLouseModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.ageInTicks = 7.25F;
        model.setupAnim(state);

        ModelPart body4 = root.getChild("body_4");
        float phase4 = 7.25F * 0.9F + 4 * 0.15F * Mth.PI;
        assertEquals(Mth.cos(phase4) * Mth.PI * 0.05F * 3, body4.yRot, EPSILON);
        assertEquals(Mth.sin(phase4) * Mth.PI * 0.2F * 2, body4.x, EPSILON);
        assertEquals(body4.yRot, root.getChild("fin_1").yRot, EPSILON);
        assertEquals(body4.x, root.getChild("fin_1").x, EPSILON);
        assertEquals(root.getChild("body_1").yRot, root.getChild("fin_2").yRot, EPSILON);
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
            if (value instanceof Vector3f normal) {
                return normal;
            }
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
