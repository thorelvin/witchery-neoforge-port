package com.emoniph.witchery.client.model;

import static com.emoniph.witchery.client.model.LegacyModelTestSupport.assertUvBounds;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubes;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubesAtPath;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.mirroredCubeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class BarkGrowthModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void restoresAllTwentyUnmirroredSourcePiecesAndTheirChargeGroups() {
        ModelPart tallRoot = BarkGrowthModel.createTallUvLayer().bakeRoot();
        ModelPart wideRoot = BarkGrowthModel.createWideUvLayer().bakeRoot();
        BarkGrowthModel model = new BarkGrowthModel(tallRoot, wideRoot);

        assertEquals(13, tallRoot.getAllParts().size());
        assertEquals(9, wideRoot.getAllParts().size());
        assertEquals(12, cubes(tallRoot).size());
        assertEquals(8, cubes(wideRoot).size());
        assertEquals(0, mirroredCubeCount(tallRoot));
        assertEquals(0, mirroredCubeCount(wideRoot));
        assertEquals(2, model.parts(BarkGrowthModel.Group.BODY).size());
        assertEquals(4, model.parts(BarkGrowthModel.Group.SPIKES).size());
        assertEquals(2, model.parts(BarkGrowthModel.Group.RIGHT_LEG).size());
        assertEquals(2, model.parts(BarkGrowthModel.Group.LEFT_LEG).size());
        assertEquals(3, model.parts(BarkGrowthModel.Group.RIGHT_ARM).size());
        assertEquals(3, model.parts(BarkGrowthModel.Group.LEFT_ARM).size());
        assertEquals(2, model.parts(BarkGrowthModel.Group.RIGHT_HEAD_SHOULDER).size());
        assertEquals(2, model.parts(BarkGrowthModel.Group.LEFT_HEAD_SHOULDER).size());
    }

    @Test
    void tallUvPiecesMatchEverySourceCubePivotAndRotation() {
        ModelPart root = BarkGrowthModel.createTallUvLayer().bakeRoot();
        for (PartSpec spec : tallParts()) assertPart(root, spec);

        assertUvBounds(cube(root, "head_right"), 124.0F / 64.0F, 0.0F, 128.0F / 64.0F, 6.0F / 128.0F);
        assertUvBounds(cube(root, "arm_right_front"),
                102.0F / 64.0F, 0.0F, 110.0F / 64.0F, 8.0F / 128.0F);
    }

    @Test
    void wideUvPiecesMatchEverySourceCubePivotAndRotation() {
        ModelPart root = BarkGrowthModel.createWideUvLayer().bakeRoot();
        for (PartSpec spec : wideParts()) assertPart(root, spec);

        assertUvBounds(cube(root, "arm_left_out"),
                120.0F / 128.0F, 0.0F, 128.0F / 128.0F, 10.0F / 64.0F);
        assertUvBounds(cube(root, "shoulder_right"),
                108.0F / 128.0F, 0.0F, 128.0F / 128.0F, 6.0F / 64.0F);
    }

    private static List<PartSpec> tallParts() {
        return List.of(
                spec("head_right", -4.0F, 0.0F, 0.0F, -0.1487144F, 0.0F, -0.4089647F,
                        -0.5F, -5.0F, -0.5F, 0.5F, 0.0F, 0.5F),
                spec("head_left", 4.0F, 0.0F, 0.0F, -0.1487144F, 0.0F, 0.4089647F,
                        -0.5F, -5.0F, -0.5F, 0.5F, 0.0F, 0.5F),
                spec("leg_right_front", -4.0F, 13.0F, -3.0F, 0.0F, 0.0F, -0.2230717F,
                        0.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.0F),
                spec("leg_left_front", 1.0F, 13.0F, -3.0F, 0.0F, 0.0F, 0.1230717F,
                        0.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.0F),
                spec("leg_right_back", -4.0F, 13.0F, 2.0F, 0.0F, 0.0F, -0.2230717F,
                        0.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.0F),
                spec("leg_left_back", 1.0F, 13.0F, 2.0F, 0.0F, 0.0F, 0.1230717F,
                        0.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.0F),
                spec("body_front", -2.5F, 1.0F, -3.1F, 0.0F, 0.0F, 0.1487144F,
                        0.0F, 0.0F, 0.0F, 6.0F, 9.0F, 1.0F),
                spec("body_back", -2.5F, 1.0F, 2.1F, 0.0F, 0.0F, 0.0887144F,
                        0.0F, 0.0F, 0.0F, 6.0F, 9.0F, 1.0F),
                spec("arm_right_front", -8.0F, 3.0F, -3.0F, 0.0F, 0.0F, -0.1487144F,
                        0.0F, 0.0F, 0.0F, 3.0F, 7.0F, 1.0F),
                spec("arm_left_front", 5.0F, 2.0F, -3.0F, 0.0F, 0.0F, 0.0687144F,
                        0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 1.0F),
                spec("arm_right_back", -8.0F, 3.0F, 2.0F, 0.0F, 0.0F, -0.1487144F,
                        0.0F, 0.0F, 0.0F, 3.0F, 7.0F, 1.0F),
                spec("arm_left_back", 5.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0687144F,
                        0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 1.0F));
    }

    private static List<PartSpec> wideParts() {
        return List.of(
                spec("arm_left_out", 8.0F, 2.0F, -1.5F, 0.0371786F, 0.0F, 0.0F,
                        0.0F, 0.0F, 0.0F, 1.0F, 7.0F, 3.0F),
                spec("arm_right_out", -9.0F, 2.0F, -1.0F, -0.1858931F, 0.0F, 0.0F,
                        0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 3.0F),
                spec("spike_lower_right", -1.0F, 7.0F, 2.0F, -0.7807508F, -0.1858931F, 0.0F,
                        -0.5F, -6.0F, -0.5F, 0.5F, 0.0F, 0.5F),
                spec("spike_lower_left", 1.0F, 7.0F, 2.0F, -0.7807508F, 0.1858931F, 0.0F,
                        -0.5F, -6.0F, -0.5F, 0.5F, 0.0F, 0.5F),
                spec("spike_upper_left", 2.0F, 3.0F, 2.0F, -0.7807508F, 0.1858931F, 0.0F,
                        -0.5F, -6.0F, -0.5F, 0.5F, 0.0F, 0.5F),
                spec("spike_upper_right", -2.0F, 3.0F, 2.0F, -0.7807508F, -0.1858931F, 0.0F,
                        -0.5F, -6.0F, -0.5F, 0.5F, 0.0F, 0.5F),
                spec("shoulder_right", -9.0F, -1.5F, -2.5F, 0.0371786F, -0.1115358F, -0.1230717F,
                        0.0F, 0.0F, 0.0F, 5.0F, 1.0F, 5.0F),
                spec("shoulder_left", 4.0F, -2.5F, -1.5F, 0.0F, 0.2974289F, 0.1830717F,
                        0.0F, 0.0F, 0.0F, 5.0F, 1.0F, 5.0F));
    }

    private static void assertPart(ModelPart root, PartSpec spec) {
        ModelPart part = root.getChild(spec.name());
        assertEquals(spec.pivotX(), part.x, EPSILON, spec.name() + " pivot x");
        assertEquals(spec.pivotY(), part.y, EPSILON, spec.name() + " pivot y");
        assertEquals(spec.pivotZ(), part.z, EPSILON, spec.name() + " pivot z");
        assertEquals(spec.xRot(), part.xRot, EPSILON, spec.name() + " x rotation");
        assertEquals(spec.yRot(), part.yRot, EPSILON, spec.name() + " y rotation");
        assertEquals(spec.zRot(), part.zRot, EPSILON, spec.name() + " z rotation");
        ModelPart.Cube cube = cube(root, spec.name());
        assertEquals(spec.minX(), cube.minX, EPSILON, spec.name() + " min x");
        assertEquals(spec.minY(), cube.minY, EPSILON, spec.name() + " min y");
        assertEquals(spec.minZ(), cube.minZ, EPSILON, spec.name() + " min z");
        assertEquals(spec.maxX(), cube.maxX, EPSILON, spec.name() + " max x");
        assertEquals(spec.maxY(), cube.maxY, EPSILON, spec.name() + " max y");
        assertEquals(spec.maxZ(), cube.maxZ, EPSILON, spec.name() + " max z");
    }

    private static ModelPart.Cube cube(ModelPart root, String name) {
        List<ModelPart.Cube> cubes = cubesAtPath(root.getChild(name), "");
        assertEquals(1, cubes.size(), name + " cube count");
        return cubes.getFirst();
    }

    private static PartSpec spec(String name, float pivotX, float pivotY, float pivotZ,
                                 float xRot, float yRot, float zRot,
                                 float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ) {
        return new PartSpec(name, pivotX, pivotY, pivotZ, xRot, yRot, zRot,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    private record PartSpec(String name, float pivotX, float pivotY, float pivotZ,
                            float xRot, float yRot, float zRot,
                            float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ) {
    }
}
