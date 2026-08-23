package com.emoniph.witchery.client.model;

import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubes;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubesAtPath;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.mirroredCubeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class LegacyHeldItemModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void brewBottleRetainsTheThreeMirroredBottleCubesAndLateUnmirroredStopper() {
        ModelPart root = BrewBottleModel.createLayer().bakeRoot();
        ModelPart bottle = root.getChild("bottle");
        ModelPart stopper = root.getChild("stopper");

        assertEquals(3, root.getAllParts().size());
        assertEquals(4, cubes(root).size());
        assertEquals(3, mirroredCubeCount(root));
        assertPose(bottle, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, "bottle");
        assertPose(stopper, -1.0F, -4.5F, -1.0F, 0.0F, 0.0F, 0.0F, "stopper");

        List<ModelPart.Cube> bottleCubes = cubesAtPath(bottle, "");
        assertEquals(3, bottleCubes.size());
        assertBounds(bottleCubes.get(0), -1.5F, -2.0F, -1.5F, 1.5F, 0.0F, 1.5F, "body outer");
        assertBounds(bottleCubes.get(1), -1.0F, -2.5F, -1.0F, 1.0F, 0.5F, 1.0F, "body inner");
        assertBounds(bottleCubes.get(2), -0.5F, -4.0F, -0.5F, 0.5F, -2.0F, 0.5F, "neck");
        assertBounds(cubesAtPath(stopper, "").getFirst(),
                0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F, "stopper");
    }

    @Test
    void caneSwordRetainsEverySourceCubeAndPivotWithoutInventedMirroring() {
        ModelPart root = CaneSwordModel.createLayer().bakeRoot();

        assertEquals(6, root.getAllParts().size());
        assertEquals(5, cubes(root).size());
        assertEquals(0, mirroredCubeCount(root));
        assertPart(root, spec("blade_3", -5.8F, 11.0F, -1.6F,
                -1.0F, -17.0F, -1.0F, 1.0F, -15.0F, 1.0F));
        assertPart(root, spec("blade_2", -5.8F, 11.0F, -1.6F,
                -1.0F, -15.0F, -1.0F, 1.0F, -11.0F, 1.0F));
        assertPart(root, spec("ball", -5.8F, 10.0F, -1.6F,
                -1.5F, -1.0F, -1.5F, 1.5F, 1.0F, 1.5F));
        assertPart(root, spec("blade_1", -5.8F, 8.0F, -1.6F,
                -1.0F, -8.0F, -1.0F, 1.0F, 1.0F, 1.0F));
        assertPart(root, spec("sheath", -5.8F, 11.0F, -1.6F,
                -1.0F, 0.0F, -1.0F, 1.0F, 13.0F, 1.0F));
    }

    @Test
    void handBowRetainsAllTwentyTwoMirroredSourceParts() {
        ModelPart root = HandBowModel.createLayer().bakeRoot();

        assertEquals(23, root.getAllParts().size());
        assertEquals(22, cubes(root).size());
        assertEquals(22, mirroredCubeCount(root));
        for (PartSpec spec : handBowParts()) {
            assertPart(root, spec);
        }
    }

    @Test
    void handBowDrawStagesAndBoltVisibilityMatchTheSourceStateMachine() {
        ModelPart root = HandBowModel.createLayer().bakeRoot();
        HandBowModel model = new HandBowModel(root);

        model.setup(0, HandBowModel.Bolt.NONE);
        assertDrawPositions(root, -7.0F, -6.0F, -4.0F, -4.0F, -4.0F, -3.25F);
        assertVisibleBolts(root);

        model.setup(1, HandBowModel.Bolt.STAKE);
        assertDrawPositions(root, -6.0F, -5.0F, -2.0F, -2.0F, -3.0F, -1.0F);
        assertVisibleBolts(root, "bolt_stake");

        model.setup(2, HandBowModel.Bolt.SPLITTING);
        assertDrawPositions(root, -6.0F, -4.0F, -1.0F, 0.0F, -2.0F, 1.0F);
        assertVisibleBolts(root, "bolt_splitting", "bolt_splitting_2");

        model.setup(99, HandBowModel.Bolt.SILVER);
        assertDrawPositions(root, -6.0F, -4.0F, -1.0F, 0.0F, -2.0F, 1.0F);
        assertVisibleBolts(root, "bolt_silver");
    }

    @Test
    void huntsmanSpearRetainsItsMirroredShaftAndCrossedZeroDepthHeadPlanes() {
        ModelPart root = HuntsmanSpearModel.createLayer().bakeRoot();

        assertEquals(4, root.getAllParts().size());
        assertEquals(3, cubes(root).size());
        assertEquals(3, mirroredCubeCount(root));
        assertPart(root, spec("shaft", 0.0F, 0.0F, 0.0F,
                -0.5F, -18.0F, -0.5F, 0.5F, 14.0F, 0.5F));
        assertPart(root, spec("head_front", 0.0F, -17.0F, 0.0F,
                -1.5F, -6.0F, 0.0F, 1.5F, 0.0F, 0.0F));
        assertPart(root, spec("head_side", 0.0F, -17.0F, 0.0F,
                0.0F, -6.0F, -1.5F, 0.0F, 0.0F, 1.5F));
    }

    @Test
    void mysticBranchRetainsEverySourcePivotRotationAndMirroredCube() {
        ModelPart root = MysticBranchModel.createLayer().bakeRoot();

        assertEquals(7, root.getAllParts().size());
        assertEquals(6, cubes(root).size());
        assertEquals(6, mirroredCubeCount(root));
        assertPart(root, spec("branch_1", 0.0F, -8.0F, 0.0F, 0.1858931F, 0.0F, 0.2230717F,
                0.0F, -4.0F, 0.0F, 1.0F, 0.0F, 1.0F));
        assertPart(root, spec("branch_2", 1.0F, -9.0F, 1.0F, -0.2230717F, 0.0F, -0.4089647F,
                -0.5F, -5.2F, -1.2F, 0.5F, -0.2F, -0.2F));
        assertPart(root, spec("branch_3", 1.0F, -4.0F, 0.0F, 0.2230717F, -0.0371786F, 0.4089647F,
                -1.0F, -6.0F, 0.0F, 0.0F, 0.0F, 1.0F));
        assertPart(root, spec("branch_4", 1.0F, -4.9F, 1.0F, -0.5948578F, 0.0F, -0.4089647F,
                -1.0F, -3.5F, -0.5F, 0.0F, 0.5F, 0.5F));
        assertPart(root, spec("branch_5", 1.0F, -12.0F, -1.0F, -0.3717861F, 0.0F, 0.0F,
                -0.2F, -4.8F, 0.4F, 0.8F, 0.2F, 1.4F));
        assertPart(root, spec("branch_6", 0.0F, 0.0F, 0.0F,
                0.0F, -8.0F, 0.0F, 1.0F, 0.0F, 1.0F));
    }

    private static List<PartSpec> handBowParts() {
        return List.of(
                spec("stock_top", 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -5.0F, 1.0F, 1.0F, 2.0F),
                spec("stock_bottom", 0.0F, 1.0F, 0.0F, -0.5F, 0.0F, -6.0F, 0.5F, 1.0F, 2.0F),
                spec("stock_catch", 0.0F, -1.0F, 1.0F, -0.5F, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F),
                spec("grip", 0.0F, 2.0F, 0.0F, -0.5F, 0.0F, -1.0F, 0.5F, 3.0F, 1.0F),
                spec("cross", 0.0F, -0.3F, -7.0F, -3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 2.0F),
                spec("cross_outer_right", -4.0F, 0.0F, -4.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 2.0F),
                spec("cross_inner_right", -3.0F, -0.15F, -6.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 2.0F),
                spec("cross_outer_left", 4.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                spec("cross_inner_left", 3.0F, -0.15F, -6.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                spec("string_inner_right", -2.0F, -0.05F, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F),
                spec("string_mid_right", -1.0F, -0.1F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F),
                spec("string_outer_right", -3.0F, 0.0F, -2.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F),
                spec("string_inner_left", 2.0F, -0.05F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                spec("string_mid_left", 1.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                spec("string_outer_left", 3.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                spec("string_center", 0.0F, -0.1F, 1.0F, 0.0F, 0.0174533F, 0.0F,
                        -1.5F, 0.0F, -0.5F, 1.5F, 1.0F, 0.5F),
                spec("bolt_stake", 0.0F, -1.0F, -2.0F, -0.5F, 0.5F, -6.0F, 0.5F, 1.5F, 3.0F),
                spec("bolt_draining", 0.0F, -1.0F, -2.0F, -0.5F, 0.5F, -6.0F, 0.5F, 1.5F, 3.0F),
                spec("bolt_holy", 0.0F, -1.0F, -2.0F, -0.5F, 0.5F, -6.0F, 0.5F, 1.5F, 3.0F),
                spec("bolt_splitting", 0.0F, -1.0F, -2.0F, -0.5F, 0.5F, -6.0F, 0.5F, 1.5F, 3.0F),
                spec("bolt_splitting_2", -0.5F, -1.5F, -1.0F, -0.5F, 0.5F, -6.0F, 1.5F, 1.5F, -2.0F),
                spec("bolt_silver", 0.0F, -1.0F, -2.0F, -0.5F, 0.5F, -6.0F, 0.5F, 1.5F, 3.0F));
    }

    private static void assertDrawPositions(ModelPart root, float innerCrossZ, float outerCrossZ,
                                            float innerStringZ, float midStringZ,
                                            float outerStringZ, float centerStringZ) {
        assertPairZ(root, "cross_inner_left", "cross_inner_right", innerCrossZ);
        assertPairZ(root, "cross_outer_left", "cross_outer_right", outerCrossZ);
        assertPairZ(root, "string_inner_left", "string_inner_right", innerStringZ);
        assertPairZ(root, "string_mid_left", "string_mid_right", midStringZ);
        assertPairZ(root, "string_outer_left", "string_outer_right", outerStringZ);
        assertEquals(centerStringZ, root.getChild("string_center").z, EPSILON, "string center z");
    }

    private static void assertPairZ(ModelPart root, String left, String right, float expected) {
        assertEquals(expected, root.getChild(left).z, EPSILON, left + " z");
        assertEquals(expected, root.getChild(right).z, EPSILON, right + " z");
    }

    private static void assertVisibleBolts(ModelPart root, String... visibleNames) {
        List<String> expected = List.of(visibleNames);
        for (String name : List.of("bolt_stake", "bolt_draining", "bolt_holy", "bolt_splitting",
                "bolt_splitting_2", "bolt_silver")) {
            if (expected.contains(name)) {
                assertTrue(root.getChild(name).visible, name + " should be visible");
            } else {
                assertFalse(root.getChild(name).visible, name + " should be hidden");
            }
        }
    }

    private static void assertPart(ModelPart root, PartSpec spec) {
        ModelPart part = root.getChild(spec.name());
        assertPose(part, spec.pivotX(), spec.pivotY(), spec.pivotZ(),
                spec.xRot(), spec.yRot(), spec.zRot(), spec.name());
        List<ModelPart.Cube> partCubes = cubesAtPath(part, "");
        assertEquals(1, partCubes.size(), spec.name() + " cube count");
        assertBounds(partCubes.getFirst(), spec.minX(), spec.minY(), spec.minZ(),
                spec.maxX(), spec.maxY(), spec.maxZ(), spec.name());
    }

    private static void assertPose(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot, String name) {
        assertEquals(x, part.x, EPSILON, name + " pivot x");
        assertEquals(y, part.y, EPSILON, name + " pivot y");
        assertEquals(z, part.z, EPSILON, name + " pivot z");
        assertEquals(xRot, part.xRot, EPSILON, name + " x rotation");
        assertEquals(yRot, part.yRot, EPSILON, name + " y rotation");
        assertEquals(zRot, part.zRot, EPSILON, name + " z rotation");
    }

    private static void assertBounds(ModelPart.Cube cube,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     String name) {
        assertEquals(minX, cube.minX, EPSILON, name + " min x");
        assertEquals(minY, cube.minY, EPSILON, name + " min y");
        assertEquals(minZ, cube.minZ, EPSILON, name + " min z");
        assertEquals(maxX, cube.maxX, EPSILON, name + " max x");
        assertEquals(maxY, cube.maxY, EPSILON, name + " max y");
        assertEquals(maxZ, cube.maxZ, EPSILON, name + " max z");
    }

    private static PartSpec spec(String name, float pivotX, float pivotY, float pivotZ,
                                 float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ) {
        return spec(name, pivotX, pivotY, pivotZ, 0.0F, 0.0F, 0.0F,
                minX, minY, minZ, maxX, maxY, maxZ);
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
