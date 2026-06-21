package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.WitchesShoesArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class WitchesShoesArmorClientExtensions implements IClientItemExtensions {
    private WitchesShoesArmorModel model;

    private WitchesShoesArmorModel model() {
        if (model == null) {
            model = new WitchesShoesArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    WitchesShoesArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        WitchesShoesArmorModel shoesModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, shoesModel.head);
            copyPose(originalHumanoid.hat, shoesModel.hat);
            copyPose(originalHumanoid.body, shoesModel.body);
            copyPose(originalHumanoid.rightArm, shoesModel.rightArm);
            copyPose(originalHumanoid.leftArm, shoesModel.leftArm);
            copyPose(originalHumanoid.rightLeg, shoesModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, shoesModel.leftLeg);
        }
        applySlotVisibility(shoesModel, equipmentSlot(stack));
        return shoesModel;
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.FEET;
    }

    private static void applySlotVisibility(WitchesShoesArmorModel model, EquipmentSlot slot) {
        model.setAllVisible(false);
        if (slot == EquipmentSlot.FEET) {
            model.rightLeg.visible = true;
            model.leftLeg.visible = true;
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
