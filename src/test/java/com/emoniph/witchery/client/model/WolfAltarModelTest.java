package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WolfAltarModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void allThirtySevenSourceCubesAndTheirHierarchyArePreserved() {
        ModelPart root = WolfAltarModel.createBodyLayer().bakeRoot();
        assertEquals(38, root.getAllParts().size());
        assertEquals(37, cubes(root).size());

        ModelPart body = assertPart(root, "biped_body", 0, 3.1F, 0, 0, 0, 0, -4, 0, -2, 4, 12, 2);
        assertPart(body, "biped_left_leg", 0, 0, 0, 0, 0, 0, 0, 12, -2, 4, 24, 2);
        ModelPart rightLeg = assertPart(body, "biped_right_leg", 0, 0, 0, -(float) Math.PI / 3, 0, 0,
                -4, 7.4F, 8, 0, 13.4F, 12);
        assertPart(rightLeg, "biped_right_leg_lower", 0, 0, 0, 0.95609134F, 0, 0,
                -4, 15.7F, -6.2F, 0, 21.7F, -2.2F);
        assertPart(body, "biped_left_arm", 0, 2, 0, -0.4553564F, -0.13665928F, 0,
                4, -2, -2, 8, 10, 2);
        ModelPart rightArm = assertPart(body, "biped_right_arm", 0, 2, 0, -2.4586453F, 0.4098033F, 0,
                -8, -2, -2, -4, 10, 2);
        assertPart(rightArm, "shaft", 0, 0, 0, 0, 0, 0, -6.5F, 8, -17.9F, -5.5F, 9, 22.1F);
        ModelPart bipedHead = assertPart(body, "wolf_head_main0", 0, 0, 0, 0.13665928F, 0.13665928F, 0,
                -3, -6, -2, 3, 0, 3);
        assertPart(bipedHead, "wolf_head_main1", 0, 0, 0, 0, 0, 0, -3, -8, 2, -1, -6, 3);
        assertPart(bipedHead, "wolf_head_main2", 0, 0, 0, 0, 0, 0, 1, -8, 2, 3, -6, 3);
        assertPart(bipedHead, "wolf_head_main3", 0, 0, 0, 0, 0, 0, -1.5F, -3.1F, -5, 1.5F, -0.1F, -1);

        assertPart(root, "shape37", -3.9F, 18, -6.5F, 0, 0, 0, 0, 0, 0, 4, 2, 4);
        assertPart(root, "shape1", -8, 22, -8, 0, 0, 0, 0, 0, 0, 16, 2, 16);
        assertPart(root, "shape1_1", -7, 21, -7, 0, 0, 0, 0, 0, 0, 14, 1, 14);
        assertPart(root, "shape1_2", -8, 20, -8, 0, 0, 0, 0, 0, 0, 16, 1, 16);

        assertWolf(root, "wolf_body", -5, 14, 3.5F, 0.27314404F, true);
        assertWolf(root, "wolf_body_1", 5.5F, 14, -2, 0.31869712F, false);
    }

    @Test
    void onlyTheFourSourceBipedLimbsAreMirrored() throws ReflectiveOperationException {
        ModelPart root = WolfAltarModel.createBodyLayer().bakeRoot();
        ModelPart body = root.getChild("biped_body");
        for (String name : List.of("biped_left_leg", "biped_right_leg", "biped_left_arm", "biped_right_arm")) {
            assertEquals(1.0F, westFaceNormal(cubes(body.getChild(name)).getFirst()).x(), EPSILON, name);
        }
        long mirrored = 0;
        for (ModelPart.Cube cube : cubes(root)) {
            if (westFaceNormal(cube).x() > 0.0F) {
                mirrored++;
            }
        }
        assertEquals(4, mirrored);
    }

    private static void assertWolf(ModelPart root, String name, float x, float y, float z,
                                   float yaw, boolean first) {
        ModelPart wolf = assertPart(root, name, x, y, z, 0, yaw, 0, -3, -2, -3, 3, 4, 6);
        float headRotation = first ? -0.22759093F : 0.0F;
        ModelPart head = assertPart(wolf, "wolf_head_main0", 0, 0, 0, headRotation, headRotation, 0,
                -3, -2.5F, -13, 3, 3.5F, -9);
        assertPart(head, "wolf_head_main1", 0, 0, 0, 0, 0, 0, -3, -4.5F, -10, -1, -2.5F, -9);
        assertPart(head, "wolf_head_main2", 0, 0, 0, 0, 0, 0, 1, -4.5F, -10, 3, -2.5F, -9);
        assertPart(head, "wolf_head_main3", 0, 0, 0, 0, 0, 0, -1.5F, 0.5F, -17.1F, 1.5F, 3.5F, -13.1F);
        assertPart(wolf, "wolf_leg1", 0, 0, 0, 0, 0, 0, -2.5F, 4, 3, -0.5F, 12, 5);
        assertPart(wolf, "wolf_leg2", 0, 0, 0, 0, 0, 0, 0.5F, 4, 3, 2.5F, 12, 5);
        float legYaw = first ? -0.091106184F : 0.0F;
        assertPart(wolf, "wolf_leg3", 0, 0, 0, 0, legYaw, 0, -2.5F, 4, -8, -0.5F, 12, -6);
        assertPart(wolf, "wolf_leg4", 0, 0, 0, 0, legYaw, 0, 0.5F, 4, -8, 2.5F, 12, -6);
        assertPart(wolf, "wolf_tail", 0, 0.6F, 0, 0.31869712F, 0, 0, -1, 0, 4, 1, 8, 6);
        assertPart(wolf, "wolf_mane", 0, 0, 0, 0, first ? -0.13665928F : 0, 0,
                -4, -3, -9, 4, 4, -3);
    }

    private static ModelPart assertPart(ModelPart parent, String name,
                                        float pivotX, float pivotY, float pivotZ,
                                        float xRot, float yRot, float zRot,
                                        float minX, float minY, float minZ,
                                        float maxX, float maxY, float maxZ) {
        ModelPart part = parent.getChild(name);
        assertEquals(pivotX, part.x, EPSILON, name + " pivotX");
        assertEquals(pivotY, part.y, EPSILON, name + " pivotY");
        assertEquals(pivotZ, part.z, EPSILON, name + " pivotZ");
        assertEquals(xRot, part.xRot, EPSILON, name + " xRot");
        assertEquals(yRot, part.yRot, EPSILON, name + " yRot");
        assertEquals(zRot, part.zRot, EPSILON, name + " zRot");
        ModelPart.Cube cube = cubes(part).getFirst();
        assertEquals(minX, cube.minX, EPSILON, name + " minX");
        assertEquals(minY, cube.minY, EPSILON, name + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, name + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, name + " maxZ");
        return part;
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
