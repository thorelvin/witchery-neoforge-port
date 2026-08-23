package com.emoniph.witchery.client.model;

import static com.emoniph.witchery.client.model.LegacyModelTestSupport.assertRenderedBounds;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubes;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubesAtPath;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.mirroredCubeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class WitchesClothesArmorModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void witchesHatKeepsInflatedBipedAndFourMirroredHatCubes() {
        ModelPart root = WitchesHatArmorModel.createLayer().bakeRoot();
        WitchesHatArmorModel model = new WitchesHatArmorModel(root);
        assertEquals(9, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertEquals(6, mirroredCubeCount(root));
        assertRenderedBounds(cubesAtPath(root.getChild("head"), "").getFirst(),
                -4.61F, -8.61F, -4.61F, 4.61F, 0.61F, 4.61F);
        assertRenderedBounds(cubesAtPath(model.hat, "").getFirst(),
                -5.11F, -9.11F, -5.11F, 5.11F, 1.11F, 5.11F);
        assertEquals(4, cubesAtPath(root.getChild("head").getChild("witches_hat"), "").size());
    }

    @Test
    void babasHatKeepsInflatedBipedAndUnmirroredCrookedStack() {
        ModelPart root = BabasHatArmorModel.createLayer().bakeRoot();
        assertEquals(12, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertEquals(2, mirroredCubeCount(root));
        ModelPart brim = root.getChild("head").getChild("babas_brim");
        assertPose(brim, -7, -8, -7, 0, 0, 0);
        assertRenderedBounds(cubesAtPath(brim, "").getFirst(),
                -0.52F, -0.52F, -0.52F, 14.52F, 2.52F, 14.52F);
        assertPose(brim.getChild("babas_crown"), 3.75F, -4, 4,
                -0.05235988F, 0, 0.02617994F);
    }

    @Test
    void robeAndNecromancerShouldersKeepSourceHierarchyAndWinding() {
        ModelPart robeRoot = WitchesRobeArmorModel.createLayer().bakeRoot();
        assertEquals(9, robeRoot.getAllParts().size());
        assertEquals(8, cubes(robeRoot).size());
        assertEquals(2, mirroredCubeCount(robeRoot));
        assertPose(robeRoot.getChild("body").getChild("lower_torso"), 0, 12, 0, 0, 0, 0);

        ModelPart necroRoot = NecromancerRobeArmorModel.createLayer().bakeRoot();
        assertEquals(11, necroRoot.getAllParts().size());
        assertEquals(10, cubes(necroRoot).size());
        assertEquals(2, mirroredCubeCount(necroRoot));
        assertPose(necroRoot.getChild("right_arm").getChild("shoulder_plate"), -4, -2, -3, 0, 0, 0);
        assertPose(necroRoot.getChild("left_arm").getChild("shoulder_plate"), 0, -2, -3, 0, 0, 0);
    }

    @Test
    void shoesUseChestScaleWhileBeltsUseLeggingsScale() {
        ModelPart shoeRoot = WitchesShoesArmorModel.createLayer().bakeRoot();
        assertRenderedBounds(cubesAtPath(shoeRoot.getChild("right_leg"), "").getFirst(),
                -2.61F, -0.61F, -2.61F, 2.61F, 12.61F, 2.61F);

        ModelPart beltRoot = WitchesBeltArmorModel.createLayer().bakeRoot();
        assertRenderedBounds(cubesAtPath(beltRoot.getChild("body"), "").getFirst(),
                -4.45F, -0.45F, -2.45F, 4.45F, 12.45F, 2.45F);
        assertEquals(2, mirroredCubeCount(beltRoot));
    }

    private static void assertPose(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
    }
}
