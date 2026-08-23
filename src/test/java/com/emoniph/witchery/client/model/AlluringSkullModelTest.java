package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class AlluringSkullModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void vanillaSkeletonHeadGeometryUsedByTheSourceIsPreserved() {
        ModelPart root = AlluringSkullModel.createBodyLayer().bakeRoot();
        ModelPart head = root.getChild("head");
        ModelPart.Cube cube = cubes(head).getFirst();

        assertEquals(2, root.getAllParts().size());
        assertEquals(0.0F, head.x, EPSILON);
        assertEquals(0.0F, head.y, EPSILON);
        assertEquals(0.0F, head.z, EPSILON);
        assertEquals(-4.0F, cube.minX, EPSILON);
        assertEquals(-8.0F, cube.minY, EPSILON);
        assertEquals(-4.0F, cube.minZ, EPSILON);
        assertEquals(4.0F, cube.maxX, EPSILON);
        assertEquals(0.0F, cube.maxY, EPSILON);
        assertEquals(4.0F, cube.maxZ, EPSILON);
        assertEquals(1, cubes(root).size());
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }
}
