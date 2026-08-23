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

class DarkMarkModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSkullGeometryAndMirrorWindingArePreserved() throws ReflectiveOperationException {
        ModelPart root = DarkMarkModel.createBodyLayer().bakeRoot();
        ModelPart skull = root.getChild("skull");
        List<ModelPart.Cube> cubes = cubes(skull);
        assertEquals(2, root.getAllParts().size());
        assertEquals(2, cubes.size());
        assertEquals(20.0F, skull.y, EPSILON);
        assertCube(cubes.get(0), -8, -12, -8, 8, 0, 8);
        assertCube(cubes.get(1), -5, 0, -8, 5, 4, 4);
        for (ModelPart.Cube cube : cubes) {
            assertEquals(1.0F, westFaceNormal(cube).x(), EPSILON);
        }
    }

    @Test
    void skullTracksSourceLookYawAndPitchRatherThanSpinningWithAge() {
        ModelPart root = DarkMarkModel.createBodyLayer().bakeRoot();
        DarkMarkModel model = new DarkMarkModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.yRot = 47.5F;
        state.xRot = -18.0F;
        state.ageInTicks = 900.0F;
        model.setupAnim(state);
        assertEquals(47.5F * Mth.DEG_TO_RAD, root.getChild("skull").yRot, EPSILON);
        assertEquals(-18.0F * Mth.DEG_TO_RAD, root.getChild("skull").xRot, EPSILON);
    }

    private static void assertCube(ModelPart.Cube cube, float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
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
