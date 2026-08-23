package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class WolfHeadModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceHeadEarsAndSnoutShareTheSingleHeadPart() {
        ModelPart root = WolfHeadModel.createBodyLayer().bakeRoot();
        ModelPart head = root.getChild("head");
        List<ModelPart.Cube> cubes = cubes(head);

        assertEquals(2, root.getAllParts().size());
        assertEquals(4, cubes.size());
        assertCube(cubes.get(0), -3, -6, 0, 3, 0, 4);
        assertCube(cubes.get(1), -3, -8, 3, -1, -6, 4);
        assertCube(cubes.get(2), 1, -8, 3, 3, -6, 4);
        assertCube(cubes.get(3), -1.5F, -3.1F, -3, 1.5F, -0.1F, 1);
        assertEquals(0.0F, head.x, EPSILON);
        assertEquals(0.0F, head.y, EPSILON);
        assertEquals(0.0F, head.z, EPSILON);
    }

    private static void assertCube(ModelPart.Cube cube,
                                   float minX, float minY, float minZ,
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
}
