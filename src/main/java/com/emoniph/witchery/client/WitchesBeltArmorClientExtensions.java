package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.WitchesBeltArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Supplies the source 0.45-deformation body used by Witchery's armor-type-2 belts. */
public class WitchesBeltArmorClientExtensions implements IClientItemExtensions {
    private WitchesBeltArmorModel model;

    private WitchesBeltArmorModel model() {
        if (model == null) {
            model = new WitchesBeltArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    WitchesBeltArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        WitchesBeltArmorModel beltModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, beltModel.head);
            copyPose(originalHumanoid.hat, beltModel.hat);
            copyPose(originalHumanoid.body, beltModel.body);
            copyPose(originalHumanoid.rightArm, beltModel.rightArm);
            copyPose(originalHumanoid.leftArm, beltModel.leftArm);
            copyPose(originalHumanoid.rightLeg, beltModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, beltModel.leftLeg);
        }
        beltModel.setAllVisible(false);
        beltModel.body.visible = true;
        return beltModel;
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
