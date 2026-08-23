package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class LeechChestModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartPivotsAndCubeBoundsArePreserved() {
        ModelPart root = LeechChestModel.createBodyLayer().bakeRoot();

        assertEquals(9, root.getAllParts().size());
        assertPart(root, "chest_below", 1, 7, 1, 0, 0, 0, 14, 9, 14);
        assertPart(root, "chest_lid_bl", 14, 7, 14, -6, -5, -6, 0, 0, 0);
        assertPart(root, "chest_lid_fr", 2, 7, 2, 0, -5, 0, 6, 0, 6);
        assertPart(root, "chest_lid_br", 2, 7, 14, 0, -5, -6, 6, 0, 0);
        assertPart(root, "chest_lid_fl", 14, 7, 2, -6, -5, 0, 0, 0, 6);
        assertPart(root, "sac1", 3, 8, 0, 0, 0, 0, 2, 3, 1);
        assertPart(root, "sac2", 9, 13, 0, 0, 0, 0, 3, 2, 1);
        assertPart(root, "sac3", 9, 9, 0, 0, 0, 0, 2, 1, 1);
    }

    @Test
    void lidQuadrantsUseSourceCubicEasingAndThreeAxisRotations() {
        ModelPart root = LeechChestModel.createBodyLayer().bakeRoot();
        LeechChestModel model = new LeechChestModel(root);

        model.setLidAngle(0.4F);

        float inverse = 0.6F;
        float full = (1.0F - inverse * inverse * inverse) * Mth.HALF_PI;
        float wide = full / 1.5F;
        float narrow = full / 3.0F;
        assertRotation(root.getChild("chest_lid_bl"), -wide, narrow, wide);
        assertRotation(root.getChild("chest_lid_br"), -wide, -narrow, -wide);
        assertRotation(root.getChild("chest_lid_fl"), wide, -narrow, wide);
        assertRotation(root.getChild("chest_lid_fr"), wide, narrow, -wide);
    }

    @Test
    void sacVisibilityMatchesStoredPlayerCount() {
        ModelPart root = LeechChestModel.createBodyLayer().bakeRoot();
        LeechChestModel model = new LeechChestModel(root);

        model.setSacCount(0);
        assertFalse(root.getChild("sac1").visible);
        assertFalse(root.getChild("sac2").visible);
        assertFalse(root.getChild("sac3").visible);

        model.setSacCount(2);
        assertTrue(root.getChild("sac1").visible);
        assertTrue(root.getChild("sac2").visible);
        assertFalse(root.getChild("sac3").visible);

        model.setSacCount(3);
        assertTrue(root.getChild("sac3").visible);
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
