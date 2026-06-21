package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.NecromancerRobeArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class NecromancerRobeArmorClientExtensions implements IClientItemExtensions {
    private NecromancerRobeArmorModel model;

    private NecromancerRobeArmorModel model() {
        if (model == null) {
            model = new NecromancerRobeArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    NecromancerRobeArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        NecromancerRobeArmorModel robeModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, robeModel.head);
            copyPose(originalHumanoid.hat, robeModel.hat);
            copyPose(originalHumanoid.body, robeModel.body);
            copyPose(originalHumanoid.rightArm, robeModel.rightArm);
            copyPose(originalHumanoid.leftArm, robeModel.leftArm);
            copyPose(originalHumanoid.rightLeg, robeModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, robeModel.leftLeg);
        }
        applySlotVisibility(robeModel, equipmentSlot(stack));
        return robeModel;
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.CHEST;
    }

    private static void applySlotVisibility(NecromancerRobeArmorModel model, EquipmentSlot slot) {
        model.setAllVisible(false);
        if (slot == EquipmentSlot.CHEST) {
            model.body.visible = true;
            model.rightArm.visible = true;
            model.leftArm.visible = true;
        }
    }

    private static void copyPose(net.minecraft.client.model.geom.ModelPart from,
                                 net.minecraft.client.model.geom.ModelPart to) {
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
