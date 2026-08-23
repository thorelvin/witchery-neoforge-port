package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

class DeathHandModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void thinBonesRetainTheirFullConnectionAxis() {
        ModelPart root = DeathHandModel.createLayer().bakeRoot();

        assertZBounds(root, "palm_upper", 0.0F, 5.0F);
        assertZBounds(root, "palm_lower", 0.0F, 3.0F);
        assertZBounds(root, "finger_1_upper", -2.0F, 2.0F);
        assertZBounds(root, "finger_1_lower", -4.0F, 0.0F);
        assertZBounds(root, "right_finger_upper", -4.0F, 0.0F);
        assertZBounds(root, "right_finger_lower", 0.0F, 4.0F);
        assertZBounds(root, "right_thumb_upper", 0.0F, 3.0F);
    }

    @Test
    void sourcePartPivotsAndRenderWithRotationOrderArePreserved() {
        ModelPart root = DeathHandModel.createLayer().bakeRoot();

        assertSourcePose(root, "wrist", -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        assertSourcePose(root, "palm_upper", -3.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F);
        assertSourcePose(root, "palm_lower", -3.0F, 1.0F, -3.0F, 0.0F, 0.0F, 0.0F);
        assertSourcePose(root, "finger_1_upper", -3.0F, 1.0F, -7.0F, 0.4833219F, 0.0F, 0.0F);
        assertSourcePose(root, "finger_2_upper", -1.0F, 1.0F, -7.0F, 0.4833219F, 0.0F, 0.0F);
        assertSourcePose(root, "finger_3_upper", 1.0F, 1.0F, -7.0F, 0.4833219F, 0.0F, 0.0F);
        assertSourcePose(root, "finger_1_lower", -3.0F, 2.0F, -9.0F, 2.044824F, 0.0F, 0.0F);
        assertSourcePose(root, "finger_2_lower", -1.0F, 2.0F, -9.0F, 2.044824F, 0.0F, 0.0F);
        assertSourcePose(root, "finger_3_lower", 1.0F, 2.0F, -9.0F, 2.044824F, 0.0F, 0.0F);
        assertSourcePose(root, "right_palm", 2.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F);
        assertSourcePose(root, "right_finger_upper", 3.0F, 0.0F, -5.0F, -0.5205006F, 0.0F, 0.0F);
        assertSourcePose(root, "right_finger_lower", 3.0F, -1.0F, -8.0F, -2.732628F, 0.0F, 0.0F);
        assertSourcePose(root, "right_thumb_upper", 3.5F, 1.0F, 0.0F, -2.7F, -0.7F, -0.669215F);
        assertSourcePose(root, "right_thumb_lower", 5.0F, 3.0F, -2.0F, -1.896109F, 0.2602503F, 0.3717861F);
        assertSourcePose(root, "left_palm", -5.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F);
        assertSourcePose(root, "left_finger_upper", -5.0F, 0.0F, -5.0F, -0.5205006F, 0.0F, 0.0F);
        assertSourcePose(root, "left_finger_lower", -5.0F, -1.0F, -8.0F, -2.732628F, 0.0F, 0.0F);
        assertSourcePose(root, "left_thumb_upper", -5.0F, 1.0F, 0.0F, -1.7F, 0.8F, 0.148711F);
        assertSourcePose(root, "left_thumb_lower", -6.0F, 4.0F, -1.0F, -2.082002F, 0.0371828F, -0.6320403F);
        assertSourcePose(root, "scythe", -6.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    @Test
    void deployedScytheUsesTheSourceRenderWithRotationOrder() {
        ModelPart root = DeathHandModel.createLayer().bakeRoot();
        DeathHandModel model = new DeathHandModel(root);

        model.setup(false, true);
        assertSourceRotation(root.getChild("scythe"), (float) Math.PI, 0.0F, (float) (-Math.PI / 2));

        model.setup(true, true);
        assertSourceRotation(root.getChild("scythe"), (float) Math.PI, (float) -Math.PI,
                (float) (-Math.PI / 2));
    }

    private static void assertZBounds(ModelPart root, String partName, float minZ, float maxZ) {
        ModelPart.Cube cube = cube(root, partName);
        assertEquals(minZ, cube.minZ, EPSILON, partName + " minZ");
        assertEquals(maxZ, cube.maxZ, EPSILON, partName + " maxZ");
    }

    private static void assertSourcePose(ModelPart root, String partName, float x, float y, float z,
                                         float xRot, float yRot, float zRot) {
        ModelPart part = root.getChild(partName);
        assertEquals(x, part.x, EPSILON, partName + " x");
        assertEquals(y, part.y, EPSILON, partName + " y");
        assertEquals(z, part.z, EPSILON, partName + " z");
        assertSourceRotation(part, xRot, yRot, zRot);
    }

    private static void assertSourceRotation(ModelPart part, float xRot, float yRot, float zRot) {
        Quaternionf expected = new Quaternionf().rotateY(yRot).rotateX(xRot).rotateZ(zRot);
        Quaternionf actual = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
        assertEquals(1.0F, Math.abs(expected.dot(actual)), EPSILON, "rotation quaternion");
    }

    private static ModelPart.Cube cube(ModelPart root, String partName) {
        return root.getChild(partName).getRandomCube(RandomSource.create(0L));
    }
}
