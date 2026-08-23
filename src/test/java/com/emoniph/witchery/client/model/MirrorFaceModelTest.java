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

class MirrorFaceModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSingleUnmirroredHeadCubeAndPivotArePreserved() throws ReflectiveOperationException {
        ModelPart root = MirrorFaceModel.createBodyLayer().bakeRoot();
        ModelPart head = root.getChild("head");
        List<ModelPart.Cube> cubes = cubes(root);
        assertEquals(2, root.getAllParts().size());
        assertEquals(1, cubes.size());
        assertEquals(0.0F, head.x, EPSILON);
        assertEquals(24.0F, head.y, EPSILON);
        assertEquals(0.0F, head.z, EPSILON);
        assertEquals(-4.0F, cubes.get(0).minX, EPSILON);
        assertEquals(-8.0F, cubes.get(0).minY, EPSILON);
        assertEquals(-4.0F, cubes.get(0).minZ, EPSILON);
        assertEquals(4.0F, cubes.get(0).maxX, EPSILON);
        assertEquals(0.0F, westFaceNormal(cubes.get(0)).x() + 1.0F, EPSILON);
    }

    @Test
    void sourceHeadTracksNetYawAndPitch() {
        ModelPart root = MirrorFaceModel.createBodyLayer().bakeRoot();
        MirrorFaceModel model = new MirrorFaceModel(root);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.yRot = 32.0F;
        state.xRot = -11.0F;
        model.setupAnim(state);
        assertEquals(32.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);
        assertEquals(-11.0F * Mth.DEG_TO_RAD, root.getChild("head").xRot, EPSILON);
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
