package com.emoniph.witchery.client.model;

import static com.emoniph.witchery.client.model.LegacyModelTestSupport.assertRenderedBounds;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubes;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.cubesAtPath;
import static com.emoniph.witchery.client.model.LegacyModelTestSupport.mirroredCubeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

class VampireArmorModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartsAndSlotSpecificDeformationsArePreserved() {
        ModelPart root = VampireArmorModel.createLayer().bakeRoot();
        assertEquals(25, root.getAllParts().size());
        assertEquals(21, cubes(root).size());
        assertEquals(3, mirroredCubeCount(root));

        ModelPart body = root.getChild("body");
        assertRenderedBounds(cubesAtPath(body.getChild("outer_body"), "").getFirst(),
                -4.3F, -0.3F, -2.3F, 4.3F, 12.3F, 2.3F);
        assertRenderedBounds(cubesAtPath(body.getChild("inner_body"), "").getFirst(),
                -4.02F, -0.02F, -2.02F, 4.02F, 12.02F, 2.02F);
        assertRenderedBounds(cubesAtPath(root.getChild("right_leg").getChild("leggings"), "").getFirst(),
                -2.02F, -0.02F, -2.02F, 2.02F, 12.02F, 2.02F);
        assertRenderedBounds(cubesAtPath(root.getChild("right_leg").getChild("boot"), "").getFirst(),
                -2.3F, -0.3F, -2.3F, 2.3F, 12.3F, 2.3F);

        assertPart(root.getChild("skirt_back"), 0, 11, 0, 0, 0, 0);
        assertPart(root.getChild("cloak_left"), 0, 0, 0,
                (float) (-Math.PI / 9.0), 0.5108652F, 0.41086525F);
        assertPart(root.getChild("cloak_right"), 0, 0, 0,
                (float) (-Math.PI / 9.0), -0.5108652F, -0.41086525F);
        assertPart(root.getChild("cloak_main"), 0, 1, 0, 0.045553092F, 0, 0);
        assertPart(root.getChild("chest"), 0, 2, 0, (float) (Math.PI / 4.0), 0, 0);
    }

    @Test
    void femaleSkirtAndCloakFollowSourcePoseBranches() {
        ModelPart root = VampireArmorModel.createLayer().bakeRoot();
        VampireArmorModel model = new VampireArmorModel(root, VampireArmorModel.Variant.FEMININE);
        model.body.xRot = 0.5F;
        model.head.xRot = -0.2F;
        model.rightLeg.xRot = 0.6F;
        model.leftLeg.xRot = -0.4F;
        model.syncSourcePose(model);
        model.applySlotVisibility(EquipmentSlot.LEGS);

        assertEquals(0.6F, root.getChild("skirt_back").xRot, EPSILON);
        assertEquals(0.3F, root.getChild("skirt_middle").xRot, EPSILON);
        assertEquals(-0.4F, root.getChild("skirt_front").xRot, EPSILON);
        assertEquals(-0.2F, root.getChild("skirt_middle2").xRot, EPSILON);
        assertEquals(8.0F, root.getChild("skirt_back").y, EPSILON);
        assertEquals(4.0F, root.getChild("skirt_back").z, EPSILON);
        assertTrue(root.getChild("skirt_back").visible);
        assertTrue(model.body.getChild("inner_body").visible);
        assertFalse(model.body.getChild("outer_body").visible);
        assertTrue(model.rightLeg.getChild("leggings").visible);
        assertFalse(model.rightLeg.getChild("boot").visible);

        model.applySlotVisibility(EquipmentSlot.CHEST);
        assertTrue(root.getChild("chest").visible);
        assertTrue(root.getChild("cloak_main").visible);
        assertEquals(0.6F, root.getChild("cloak_main").xRot, EPSILON);
        assertEquals(-0.35F, root.getChild("cloak_left").xRot, EPSILON);
    }

    @Test
    void sourceRidingPoseSuppressesFemaleSkirtAndBootsUseOuterModel() {
        ModelPart root = VampireArmorModel.createLayer().bakeRoot();
        VampireArmorModel model = new VampireArmorModel(root, VampireArmorModel.Variant.FEMININE);
        model.rightLeg.xRot = -1.4137167F;
        model.leftLeg.xRot = -1.4137167F;
        model.syncSourcePose(model);
        model.applySlotVisibility(EquipmentSlot.LEGS);
        assertFalse(root.getChild("skirt_back").visible);

        model.applySlotVisibility(EquipmentSlot.FEET);
        assertTrue(model.rightLeg.getChild("boot").visible);
        assertFalse(model.rightLeg.getChild("leggings").visible);
        assertFalse(model.body.visible);
    }

    @Test
    void nonMetalHatAndMetalHeadwearMatchSourceSelection() {
        ModelPart lightRoot = VampireArmorModel.createLayer().bakeRoot();
        VampireArmorModel light = new VampireArmorModel(lightRoot, VampireArmorModel.Variant.MASCULINE);
        light.applySlotVisibility(EquipmentSlot.HEAD);
        assertFalse(light.hat.visible);
        assertTrue(light.head.getChild("source_hat").visible);
        assertTrue(light.head.getChild("source_hat_brim").visible);

        ModelPart metalRoot = VampireArmorModel.createLayer().bakeRoot();
        VampireArmorModel metal = new VampireArmorModel(metalRoot, VampireArmorModel.Variant.METALLIC);
        metal.applySlotVisibility(EquipmentSlot.HEAD);
        assertTrue(metal.hat.visible);
        assertFalse(metal.head.getChild("source_hat").visible);

        ModelPart ladiesRoot = VampireArmorModel.createLayer().bakeRoot();
        VampireArmorModel ladies = new VampireArmorModel(
                ladiesRoot, VampireArmorModel.Variant.METALLIC_FEMININE);
        ladies.applySlotVisibility(EquipmentSlot.CHEST);
        assertTrue(ladiesRoot.getChild("chest").visible);
    }

    private static void assertPart(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
    }
}
