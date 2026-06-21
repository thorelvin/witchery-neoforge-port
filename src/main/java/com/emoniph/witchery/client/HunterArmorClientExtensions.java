package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.HunterArmorModel;
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

public class HunterArmorClientExtensions implements IClientItemExtensions {
    private HunterArmorModel model;

    private HunterArmorModel model() {
        if (model == null) {
            model = new HunterArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(HunterArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        HunterArmorModel hunterModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, hunterModel.head);
            copyPose(originalHumanoid.hat, hunterModel.hat);
            copyPose(originalHumanoid.body, hunterModel.body);
            copyPose(originalHumanoid.rightArm, hunterModel.rightArm);
            copyPose(originalHumanoid.leftArm, hunterModel.leftArm);
            copyPose(originalHumanoid.rightLeg, hunterModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, hunterModel.leftLeg);
        }
        applySlotVisibility(hunterModel, equipmentSlot(stack));
        return hunterModel;
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.CHEST;
    }

    private static void applySlotVisibility(HunterArmorModel model, EquipmentSlot slot) {
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
            case LEGS -> showLegGroup(model, "legs");
            case FEET -> showLegGroup(model, "boots");
            default -> {
            }
        }
    }

    private static void showLegGroup(HunterArmorModel model, String group) {
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        setGroupVisible(model.rightLeg, group);
        setGroupVisible(model.leftLeg, group);
    }

    private static void setGroupVisible(ModelPart leg, String shownGroup) {
        leg.getChild("legs").visible = "legs".equals(shownGroup);
        leg.getChild("boots").visible = "boots".equals(shownGroup);
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
