package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class BloodCrucibleModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceNinePieceBowlGeometryIsPreserved() {
        ModelPart root = BloodCrucibleModel.createBodyLayer().bakeRoot();

        assertEquals(10, root.getAllParts().size());
        assertCube(root, "left_lower", 2.0F, -2.0F, -2.0F, 3.0F, -1.0F, 2.0F);
        assertCube(root, "right_lower", -3.0F, -2.0F, -2.0F, -2.0F, -1.0F, 2.0F);
        assertCube(root, "front_lower", -3.0F, -2.0F, -3.0F, 3.0F, -1.0F, -2.0F);
        assertCube(root, "front_upper", -3.5F, -5.0F, -4.0F, 3.5F, -2.0F, -3.0F);
        assertCube(root, "right_upper", -4.0F, -5.0F, -3.7F, -3.0F, -2.0F, 3.3F);
        assertCube(root, "bottom", -2.0F, -1.0F, -2.0F, 2.0F, 0.0F, 2.0F);
        assertCube(root, "back_lower", -3.0F, -2.0F, 2.0F, 3.0F, -1.0F, 3.0F);
        assertCube(root, "back_upper", -3.5F, -5.0F, 3.0F, 3.5F, -2.0F, 4.0F);
        assertCube(root, "left_upper", 3.0F, -5.0F, -3.5F, 4.0F, -2.0F, 3.5F);
    }

    @Test
    void liquidPlaneUsesSourceWidthAndContinuousHeightFormula() {
        ModelPart bodyRoot = BloodCrucibleModel.createBodyLayer().bakeRoot();
        ModelPart liquidRoot = BloodCrucibleModel.createLiquidLayer().bakeRoot();
        BloodCrucibleModel model = new BloodCrucibleModel(bodyRoot, liquidRoot);
        ModelPart blood = model.blood();

        ModelPart.Cube cube = blood.getRandomCube(RandomSource.create(0L));
        assertEquals(0.0F, cube.minX, EPSILON);
        assertEquals(16.0F, cube.maxX, EPSILON);
        assertEquals(0.0F, cube.minY, EPSILON);
        assertEquals(0.0F, cube.maxY, EPSILON);
        assertEquals(0.0F, cube.minZ, EPSILON);
        assertEquals(16.0F, cube.maxZ, EPSILON);
        assertEquals(-3.0F, blood.x, EPSILON);
        assertEquals(-3.0F, blood.z, EPSILON);
        assertEquals(0.375F, blood.xScale, EPSILON);
        assertEquals(0.375F, blood.zScale, EPSILON);

        model.setBloodPercent(0.0F);
        assertEquals(-1.6F, blood.y, EPSILON);
        model.setBloodPercent(0.5F);
        assertEquals(-3.2F, blood.y, EPSILON);
        model.setBloodPercent(1.0F);
        assertEquals(-4.8F, blood.y, EPSILON);
    }

    private static void assertCube(ModelPart root, String name,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(0.0F, part.x, EPSILON, name + " pivot x");
        assertEquals(0.0F, part.y, EPSILON, name + " pivot y");
        assertEquals(0.0F, part.z, EPSILON, name + " pivot z");
        ModelPart.Cube cube = part.getRandomCube(RandomSource.create(0L));
        assertEquals(minX, cube.minX, EPSILON, name + " minX");
        assertEquals(minY, cube.minY, EPSILON, name + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, name + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, name + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, name + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, name + " maxZ");
    }
}
