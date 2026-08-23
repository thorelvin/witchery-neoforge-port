package com.emoniph.witchery.client.model;

import static com.emoniph.witchery.client.model.LegacyModelTestSupport.assertRenderedBounds;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubes;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubesAtPath;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.mirroredCubeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class GoblinClothesArmorModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void twistingBandUsesSourceChestModelScale() {
        ModelPart root = TwistingBandArmorModel.createLayer().bakeRoot();
        assertEquals(8, root.getAllParts().size());
        assertEquals(7, cubes(root).size());
        assertEquals(2, mirroredCubeCount(root));
        assertRenderedBounds(cubesAtPath(root.getChild("head"), "").getFirst(),
                -4.61F, -8.61F, -4.61F, 4.61F, 0.61F, 4.61F);
    }

    @Test
    void mogsQuiverKeepsFourUnmirroredBodyChildren() {
        ModelPart root = MogsQuiverArmorModel.createLayer().bakeRoot();
        assertEquals(12, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertEquals(2, mirroredCubeCount(root));
        ModelPart body = root.getChild("body");
        assertPart(body.getChild("quiver"), 0, 7, 2, -0.3490659F, -2, -3, 0, 2, 4, 1);
        assertPart(body.getChild("arrow1"), 0, 7, 2, -0.3490659F, 0.5F, -5, 0, 1.5F, -3, 1);
        assertPart(body.getChild("arrow2"), 0, 7, 2, -0.3490659F, -1.5F, -5, 0, -0.5F, -3, 1);
        assertPart(body.getChild("feathers"), 0, 7, 2, -0.3490659F, -2, -7, 0, 2, -5, 1);
    }

    @Test
    void gulgsGirdleUsesSourceZeroScaleLeggingsModel() {
        ModelPart root = GulgsGirdleArmorModel.createLayer().bakeRoot();
        assertEquals(8, root.getAllParts().size());
        assertEquals(7, cubes(root).size());
        assertEquals(2, mirroredCubeCount(root));
        assertRenderedBounds(cubesAtPath(root.getChild("body"), "").getFirst(),
                -4, 0, -2, 4, 12, 2);
    }

    private static void assertPart(ModelPart part, float x, float y, float z, float zRot,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
        assertRenderedBounds(cubesAtPath(part, "").getFirst(), minX, minY, minZ, maxX, maxY, maxZ);
    }
}
