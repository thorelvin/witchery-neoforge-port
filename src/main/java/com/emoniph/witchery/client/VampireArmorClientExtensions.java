package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.VampireArmorModel;
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

/** Selects the exact 0.24.1 vampire-clothing geometry and source slot subsets. */
public class VampireArmorClientExtensions implements IClientItemExtensions {
    private final VampireArmorModel.Variant variant;
    private VampireArmorModel model;

    public VampireArmorClientExtensions(VampireArmorModel.Variant variant) {
        this.variant = variant;
    }

    private VampireArmorModel model() {
        if (model == null) {
            model = new VampireArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(variant.layer), variant);
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        VampireArmorModel vampireModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, vampireModel.head);
            copyPose(originalHumanoid.hat, vampireModel.hat);
            copyPose(originalHumanoid.body, vampireModel.body);
            copyPose(originalHumanoid.rightArm, vampireModel.rightArm);
            copyPose(originalHumanoid.leftArm, vampireModel.leftArm);
            copyPose(originalHumanoid.rightLeg, vampireModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, vampireModel.leftLeg);
            vampireModel.syncSourcePose(originalHumanoid);
        }
        vampireModel.applySlotVisibility(equipmentSlot(stack));
        return vampireModel;
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.CHEST;
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
