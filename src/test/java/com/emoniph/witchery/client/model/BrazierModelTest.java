package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class BrazierModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartCountAndRepresentativeAnchorsArePreserved() {
        ModelPart root = BrazierModel.createBodyLayer().bakeRoot();

        assertEquals(16, root.getAllParts().size());
        assertPart(root, "leg1", 0.7F, 10.0F, -0.74F,
                -0.5F, 0.0F, -0.5F, 0.5F, 11.0F, 0.5F);
        assertPart(root, "foot1", 0.7F, 21.0F, -0.7F,
                -0.5F, 0.0F, -0.5F, 0.5F, 5.0F, 0.5F);
        assertPart(root, "foot_base", 0.0F, 21.0F, 0.0F,
                -1.5F, -0.5F, -1.5F, 1.5F, 0.5F, 1.5F);
        assertPart(root, "pan_base", 0.0F, 9.95F, 0.0F,
                -3.0F, 0.0F, -3.0F, 3.0F, 1.0F, 3.0F);
        assertPart(root, "pan_side1", 3.0F, 9.5F, 0.0F,
                -0.5F, -0.5F, -3.0F, 0.5F, 0.5F, 3.0F);
        assertPart(root, "pan_side2", 0.0F, 9.5F, 3.0F,
                -3.0F, -0.5F, -0.5F, 3.0F, 0.5F, 0.5F);

        float quarterTurn = (float) Math.PI / 4.0F;
        assertRotation(root.getChild("foot1"), -quarterTurn, 0.0F, -quarterTurn);
        assertRotation(root.getChild("foot2"), -quarterTurn, 0.0F, quarterTurn);
        assertRotation(root.getChild("foot3"), quarterTurn, 0.0F, quarterTurn);
        assertRotation(root.getChild("foot4"), quarterTurn, 0.0F, -quarterTurn);
    }

    @Test
    void sourceAshPlaneVisibilityAndHeightFollowIngredientCount() {
        ModelPart root = BrazierModel.createBodyLayer().bakeRoot();
        BrazierModel model = new BrazierModel(root);
        ModelPart ash = root.getChild("ash");
        ModelPart.Cube cube = ash.getRandomCube(RandomSource.create(0L));

        assertEquals(-2.5F, cube.minX, EPSILON);
        assertEquals(2.5F, cube.maxX, EPSILON);
        assertEquals(0.0F, cube.minY, EPSILON);
        assertEquals(0.0F, cube.maxY, EPSILON);
        assertEquals(-2.5F, cube.minZ, EPSILON);
        assertEquals(2.5F, cube.maxZ, EPSILON);

        model.setIngredientCount(0);
        assertFalse(ash.visible);
        assertEquals(9.7F, ash.y, EPSILON);
        model.setIngredientCount(1);
        assertTrue(ash.visible);
        assertEquals(9.7F, ash.y, EPSILON);
        model.setIngredientCount(2);
        assertEquals(9.6F, ash.y, EPSILON);
        model.setIngredientCount(3);
        assertEquals(9.5F, ash.y, EPSILON);
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " x");
        assertEquals(y, part.y, EPSILON, name + " y");
        assertEquals(z, part.z, EPSILON, name + " z");
        ModelPart.Cube cube = part.getRandomCube(RandomSource.create(0L));
        assertEquals(minX, cube.minX, EPSILON, name + " minX");
        assertEquals(minY, cube.minY, EPSILON, name + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, name + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, name + " maxZ");
    }

    private static void assertRotation(ModelPart part, float xRot, float yRot, float zRot) {
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
    }
}
