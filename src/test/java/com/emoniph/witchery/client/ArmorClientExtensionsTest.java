package com.emoniph.witchery.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emoniph.witchery.client.model.DeathArmorModel;
import com.emoniph.witchery.client.model.HunterArmorModel;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

class ArmorClientExtensionsTest {
    @Test
    void deathArmorSlotsExposeTheSameSourcePartFamilies() {
        DeathArmorModel model = new DeathArmorModel(DeathArmorModel.createLayer().bakeRoot());

        DeathArmorClientExtensions.applySlotVisibility(model, EquipmentSlot.HEAD);
        assertVisible(model, true, true, false, false, false, false, false);
        assertTrue(model.head.getChild("death_head").visible);
        assertTrue(model.head.getChild("cowl").visible);

        DeathArmorClientExtensions.applySlotVisibility(model, EquipmentSlot.CHEST);
        assertVisible(model, false, false, true, true, true, false, false);

        DeathArmorClientExtensions.applySlotVisibility(model, EquipmentSlot.FEET);
        assertVisible(model, false, false, false, false, false, true, true);
    }

    @Test
    void hunterArmorSlotsExposeTheSourceChestOrSlimLegModelParts() {
        HunterArmorModel model = new HunterArmorModel(HunterArmorModel.createLayer().bakeRoot());

        HunterArmorClientExtensions.applySlotVisibility(model, EquipmentSlot.HEAD);
        assertVisible(model, true, true, false, false, false, false, false);
        assertTrue(model.head.getChild("hat_brim").visible);

        HunterArmorClientExtensions.applySlotVisibility(model, EquipmentSlot.CHEST);
        assertVisible(model, false, false, true, true, true, false, false);

        HunterArmorClientExtensions.applySlotVisibility(model, EquipmentSlot.LEGS);
        assertVisible(model, false, false, false, false, false, true, true);
    }

    private static void assertVisible(net.minecraft.client.model.HumanoidModel<?> model,
                                      boolean head, boolean hat, boolean body,
                                      boolean rightArm, boolean leftArm,
                                      boolean rightLeg, boolean leftLeg) {
        assertState(model.head.visible, head);
        assertState(model.hat.visible, hat);
        assertState(model.body.visible, body);
        assertState(model.rightArm.visible, rightArm);
        assertState(model.leftArm.visible, leftArm);
        assertState(model.rightLeg.visible, rightLeg);
        assertState(model.leftLeg.visible, leftLeg);
    }

    private static void assertState(boolean actual, boolean expected) {
        if (expected) {
            assertTrue(actual);
        } else {
            assertFalse(actual);
        }
    }
}
