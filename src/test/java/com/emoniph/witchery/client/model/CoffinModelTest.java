package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CoffinModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphAndEveryCubeCoordinateArePreserved() {
        ModelPart root = CoffinModel.createBodyLayer().bakeRoot();
        ModelPart lid = root.getChild("lid");

        assertEquals(9, root.getAllParts().size());
        assertPart(lid, -7, -5, 0, -1, 0, -8, 15, 1, 8, "lid");
        assertPart(lid.getChild("lid_top"), 0, 0, 0, 1, -2, -8, 13, -1, 6, "lid_top");
        assertPart(lid.getChild("lid_mid"), 0, 0, 0, 0, -1, -8, 14, 0, 7, "lid_mid");
        assertPart(root.getChild("side_end"), 0, -4, 0, -6, 0, 6, 6, 7, 7, "side_end");
        assertPart(root.getChild("side_right"), 0, -4, 0, -7, 0, -8, -6, 7, 7, "side_right");
        assertPart(root.getChild("side_left"), 0, -4, 0, 6, 0, -8, 7, 7, 7, "side_left");
        assertPart(root.getChild("base_lower"), 0, 8, 0, -8, 0, -8, 8, 1, 8, "base_lower");
        assertPart(root.getChild("base"), 0, 3, 0, -7, 0, -8, 7, 5, 7, "base");
        assertEquals(8, cubes(root).size());
    }

    @Test
    void onlyTheSourceLeftWallIsMirrored() throws ReflectiveOperationException {
        ModelPart root = CoffinModel.createBodyLayer().bakeRoot();
        assertEquals(1.0F, westFaceNormal(cubes(root.getChild("side_left")).getFirst()).x(), EPSILON);
        for (String name : List.of("side_right", "side_end", "base_lower", "base", "lid")) {
            assertEquals(-1.0F, westFaceNormal(cubes(root.getChild(name)).getFirst()).x(), EPSILON, name);
        }
    }

    @Test
    void sourceHalfOffsetsResetAndLidReachesAFullRightAngle() {
        ModelPart root = CoffinModel.createBodyLayer().bakeRoot();
        CoffinModel model = new CoffinModel(root);

        model.setupSourcePose(BedPart.FOOT, 1.0F);
        assertEquals(1.0F, root.getChild("side_left").z, EPSILON);
        assertEquals(1.0F, root.getChild("side_right").z, EPSILON);
        assertEquals(1.0F, root.getChild("base").z, EPSILON);
        assertEquals(Mth.PI, root.getChild("side_end").yRot, EPSILON);
        assertEquals(2.0F, root.getChild("lid").getChild("lid_top").z, EPSILON);
        assertEquals(1.0F, root.getChild("lid").getChild("lid_mid").z, EPSILON);
        assertEquals(-Mth.HALF_PI, root.getChild("lid").zRot, EPSILON);

        model.setupSourcePose(BedPart.HEAD, 0.0F);
        assertEquals(0.0F, root.getChild("side_left").z, EPSILON);
        assertEquals(0.0F, root.getChild("side_end").yRot, EPSILON);
        assertEquals(0.0F, root.getChild("lid").getChild("lid_top").z, EPSILON);
        assertEquals(0.0F, root.getChild("lid").zRot, EPSILON);
    }

    @Test
    void sourceCubicLidEasingIsPreserved() {
        assertEquals(0.0F, CoffinModel.sourceLidRotation(0.0F), EPSILON);
        assertEquals(-0.875F * Mth.HALF_PI, CoffinModel.sourceLidRotation(0.5F), EPSILON);
        assertEquals(-Mth.HALF_PI, CoffinModel.sourceLidRotation(1.0F), EPSILON);
    }

    private static void assertPart(ModelPart part, float pivotX, float pivotY, float pivotZ,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ, String label) {
        assertEquals(pivotX, part.x, EPSILON, label + " pivotX");
        assertEquals(pivotY, part.y, EPSILON, label + " pivotY");
        assertEquals(pivotZ, part.z, EPSILON, label + " pivotZ");
        ModelPart.Cube cube = cubes(part).getFirst();
        assertEquals(minX, cube.minX, EPSILON, label + " minX");
        assertEquals(minY, cube.minY, EPSILON, label + " minY");
        assertEquals(minZ, cube.minZ, EPSILON, label + " minZ");
        assertEquals(maxX, cube.maxX, EPSILON, label + " maxX");
        assertEquals(maxY, cube.maxY, EPSILON, label + " maxY");
        assertEquals(maxZ, cube.maxZ, EPSILON, label + " maxZ");
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
