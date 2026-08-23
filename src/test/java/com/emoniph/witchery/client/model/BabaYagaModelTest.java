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

class BabaYagaModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceVillagerRigAndCustomPartsHaveExactGraphGeometryAndMirrorTiming()
            throws ReflectiveOperationException {
        ModelPart root = BabaYagaModel.createBodyLayer().bakeRoot();
        assertEquals(15, root.getAllParts().size());
        assertEquals(19, cubes(root).size());

        ModelPart head = root.getChild("head");
        ModelPart nose = head.getChild("nose");
        assertPose(head, 0, 0, 0, 0, 0, 0);
        assertPose(nose, 0, -2, 0, 0, 0, 0);
        assertPose(nose.getChild("nose_tip"), 0, -2, 0, 0, 0, 0);
        assertPose(head.getChild("hat_brim"), -7, -10.03125F, -7, 0, 0, 0);
        assertPose(root.getChild("arms"), 0, 3, -1, -0.75F, 0, 0);
        assertPose(root.getChild("mortar"), -7, 10, -8, 0, 0, 0);
        assertPose(root.getChild("pestle"), -3, 6, -4,
                -1.152537F, -2.305074F, 1.839205F);
        assertPose(root.getChild("cloak"), -3.5F, -0.5F, 3.5F, 0.1F, 0, 0);

        List<ModelPart.Cube> bodyCubes = cubes(root.getChild("body"));
        assertEquals(2, bodyCubes.size());
        assertCube(bodyCubes.get(0), -4, 0, -3, 4, 12, 3);
        assertCube(bodyCubes.get(1), -4, 0, -3, 4, 18, 3);
        assertCube(cubes(root.getChild("right_leg")).getFirst(), -2, 0, -2, 2, 12, 2);
        assertCube(cubes(root.getChild("left_leg")).getFirst(), -2, 0, -2, 2, 12, 2);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(5, mirrored);
    }

    @Test
    void sourceAnimationUsesVillagerLegsAndEntityIdDrivenNoseAndPestleWobble() {
        ModelPart root = BabaYagaModel.createBodyLayer().bakeRoot();
        BabaYagaModel model = new BabaYagaModel(root);
        BabaYagaModel.BabaYagaRenderState state = new BabaYagaModel.BabaYagaRenderState();
        state.entityId = 7;
        state.entityTick = 31.0F;
        state.walkAnimationPos = 2.25F;
        state.walkAnimationSpeed = 0.6F;
        state.yRot = 22.0F;
        state.xRot = -13.0F;

        model.setupAnim(state);
        float frequency = 0.07F;
        float wobbleX = Mth.sin(31.0F * frequency) * 4.5F * Mth.DEG_TO_RAD;
        float wobbleZ = Mth.cos(31.0F * frequency) * 2.5F * Mth.DEG_TO_RAD;
        assertEquals(22.0F * Mth.DEG_TO_RAD, root.getChild("head").yRot, EPSILON);
        assertEquals(-13.0F * Mth.DEG_TO_RAD, root.getChild("head").xRot, EPSILON);
        assertEquals(Mth.cos(2.25F * 0.6662F) * 1.4F * 0.6F * 0.5F,
                root.getChild("right_leg").xRot, EPSILON);
        assertEquals(Mth.cos(2.25F * 0.6662F + Mth.PI) * 1.4F * 0.6F * 0.5F,
                root.getChild("left_leg").xRot, EPSILON);

        ModelPart nose = root.getChild("head").getChild("nose");
        assertPose(nose, 0, -2, 0, wobbleX, 0, wobbleZ);
        ModelPart pestle = root.getChild("pestle");
        assertEquals(-1.152537F + wobbleX, pestle.xRot, EPSILON);
        assertEquals(1.839205F + wobbleZ, pestle.zRot, EPSILON);

        state.isHoldingItem = true;
        model.setupAnim(state);
        assertPose(nose, 0, 1, -1.5F, -0.9F, 0, wobbleZ);
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
            if (value instanceof Vector3f normal) {
                return normal;
            }
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
