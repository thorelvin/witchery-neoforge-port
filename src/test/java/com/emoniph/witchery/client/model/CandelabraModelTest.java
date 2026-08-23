package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class CandelabraModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void allSourcePartsKeepTheirPivotsAndCubeBounds() {
        ModelPart root = CandelabraModel.createBodyLayer().bakeRoot();

        assertEquals(15, root.getAllParts().size());
        assertPart(root, "candle_left", -6, 11, -1, 0, 0, 0, 2, 8, 2);
        assertPart(root, "candle_right", 4, 11, -1, 0, 0, 0, 2, 8, 2);
        assertPart(root, "candle_front", -1, 11, -6, 0, 0, 0, 2, 8, 2);
        assertPart(root, "candle_back", -1, 11, 4, 0, 0, 0, 2, 8, 2);
        assertPart(root, "candle_middle", -1, 9, -1, 0, 0, 0, 2, 13, 2);
        assertPart(root, "support_lr", -6, 19, -1, 0, 0, 0, 12, 1, 2);
        assertPart(root, "support_fb", -1, 19, -6, 0, 0, 0, 2, 1, 12);
        assertPart(root, "sconce_left", -6.5F, 17, -1.5F, 0, 0, 0, 3, 1, 3);
        assertPart(root, "sconce_right", 3.5F, 17, -1.5F, 0, 0, 0, 3, 1, 3);
        assertPart(root, "sconce_front", -1.5F, 17, -6.5F, 0, 0, 0, 3, 1, 3);
        assertPart(root, "sconce_back", -1.5F, 17, 3.5F, 0, 0, 0, 3, 1, 3);
        assertPart(root, "sconce_middle", -1.5F, 15, -1.5F, 0, 0, 0, 3, 1, 3);
        assertPart(root, "base_top", -1.5F, 22, -1.5F, 0, 0, 0, 3, 1, 3);
        assertPart(root, "base_bottom", 0, 23, 0, -2.5F, 0, -2.5F, 2.5F, 1, 2.5F);

        List<ModelPart.Cube> cubes = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        assertEquals(14, cubes.size());
    }

    private static void assertPart(ModelPart root, String name, float x, float y, float z,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ) {
        ModelPart part = root.getChild(name);
        assertEquals(x, part.x, EPSILON, name + " pivot x");
        assertEquals(y, part.y, EPSILON, name + " pivot y");
        assertEquals(z, part.z, EPSILON, name + " pivot z");
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        assertEquals(1, cubes.size(), name + " cube count");
        assertCube(cubes.getFirst(), minX, minY, minZ, maxX, maxY, maxZ, name);
    }

    private static void assertCube(ModelPart.Cube cube,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ, String label) {
        assertEquals(minX, cube.minX, EPSILON, label + " minX");
        assertEquals(minY, cube.minY, EPSILON, label + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, label + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, label + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, label + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, label + " maxZ");
    }
}
