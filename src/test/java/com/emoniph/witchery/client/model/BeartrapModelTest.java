package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class BeartrapModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceRootHierarchyAndPartCountArePreserved() {
        ModelPart root = BeartrapModel.createBodyLayer().bakeRoot();

        assertTrue(root.hasChild("plate"));
        assertTrue(root.hasChild("base"));
        assertTrue(root.hasChild("disk_left"));
        assertTrue(root.hasChild("disk_right"));
        assertTrue(root.hasChild("arm_front"));
        assertTrue(root.hasChild("arm_back"));
        assertFalse(root.hasChild("body"));
        assertEquals(21, root.getAllParts().size());

        assertJawChildren(root.getChild("arm_front"), "front");
        assertJawChildren(root.getChild("arm_back"), "back");
    }

    @Test
    void sourceCubeBoundsAndPivotsArePreserved() {
        ModelPart root = BeartrapModel.createBodyLayer().bakeRoot();

        assertPart(root.getChild("plate"), 0.0F, 24.0F, 0.0F,
                -2.0F, -1.5F, -2.0F, 2.0F, -0.5F, 2.0F);
        assertPart(root.getChild("base"), 0.0F, 23.99F, 0.0F,
                -5.0F, -1.0F, -0.5F, 5.0F, 0.0F, 0.5F);
        assertPart(root.getChild("disk_left"), 0.0F, 24.0F, 0.0F,
                3.7F, -2.0F, -1.0F, 4.7F, 0.0F, 1.0F);
        assertPart(root.getChild("disk_right"), 0.0F, 24.0F, 0.0F,
                -4.7F, -2.0F, -1.0F, -3.7F, 0.0F, 1.0F);
        assertPart(root.getChild("arm_front"), 0.0F, 23.99F, 0.0F,
                -4.5F, -1.0F, -7.0F, 4.5F, 0.0F, -6.0F);
        assertPart(root.getChild("arm_back"), 0.0F, 23.99F, 0.0F,
                -4.5F, -1.0F, 6.0F, 4.5F, 0.0F, 7.0F);
    }

    @Test
    void openAndSprungPosesUseTheExactSourceValues() {
        ModelPart root = BeartrapModel.createBodyLayer().bakeRoot();
        BeartrapModel model = new BeartrapModel(root);

        model.setClosed(false);
        assertEquals(23.2F, root.getChild("plate").y, EPSILON);
        assertEquals(0.0F, root.getChild("arm_front").xRot, EPSILON);
        assertEquals(0.0F, root.getChild("arm_back").xRot, EPSILON);

        model.setClosed(true);
        assertEquals(23.8F, root.getChild("plate").y, EPSILON);
        assertEquals(-1.2F, root.getChild("arm_front").xRot, EPSILON);
        assertEquals(1.2F, root.getChild("arm_back").xRot, EPSILON);
    }

    private static void assertJawChildren(ModelPart jaw, String suffix) {
        assertTrue(jaw.hasChild("arm_right_" + suffix));
        assertTrue(jaw.hasChild("arm_left_" + suffix));
        for (int i = 1; i <= 5; i++) {
            assertTrue(jaw.hasChild("arm_tooth_" + i + "_" + suffix));
        }
    }

    private static void assertPart(ModelPart part, float x, float y, float z,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        ModelPart.Cube cube = part.getRandomCube(RandomSource.create(0L));
        assertEquals(minX, cube.minX, EPSILON);
        assertEquals(minY, cube.minY, EPSILON);
        assertEquals(minZ, cube.minZ, EPSILON);
        assertEquals(maxX, cube.maxX, EPSILON);
        assertEquals(maxY, cube.maxY, EPSILON);
        assertEquals(maxZ, cube.maxZ, EPSILON);
    }
}
