package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.DeathArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.EnumMap;

public class DeathArmorClientExtensions implements IClientItemExtensions {
    private final EnumMap<EquipmentSlot, DeathArmorModel> models = new EnumMap<>(EquipmentSlot.class);

    private DeathArmorModel model(EquipmentSlot slot) {
        return models.computeIfAbsent(slot, ignored ->
                new DeathArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(DeathArmorModel.LAYER_LOCATION)));
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        EquipmentSlot slot = equipmentSlot(stack);
        DeathArmorModel deathModel = model(slot);
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, deathModel.head);
            copyPose(originalHumanoid.hat, deathModel.hat);
            copyPose(originalHumanoid.body, deathModel.body);
            copyPose(originalHumanoid.rightArm, deathModel.rightArm);
            copyPose(originalHumanoid.leftArm, deathModel.leftArm);
            copyPose(originalHumanoid.rightLeg, deathModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, deathModel.leftLeg);
        }
        applySlotVisibility(deathModel, slot);
        return deathModel;
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.CHEST;
    }

    static void applySlotVisibility(DeathArmorModel model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            // Source Death clothes have no leggings item; Death's Footwear owns both leg wraps.
            case LEGS, FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
    }
}
