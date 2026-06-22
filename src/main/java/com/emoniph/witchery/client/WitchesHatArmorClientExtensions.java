package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.WitchesHatArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class WitchesHatArmorClientExtensions implements IClientItemExtensions {
    private WitchesHatArmorModel model;

    private WitchesHatArmorModel model() {
        if (model == null) {
            model = new WitchesHatArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    WitchesHatArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        WitchesHatArmorModel hatModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, hatModel.head);
            copyPose(originalHumanoid.hat, hatModel.hat);
            copyPose(originalHumanoid.body, hatModel.body);
            copyPose(originalHumanoid.rightArm, hatModel.rightArm);
            copyPose(originalHumanoid.leftArm, hatModel.leftArm);
            copyPose(originalHumanoid.rightLeg, hatModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, hatModel.leftLeg);
        }
        hatModel.setAllVisible(false);
        hatModel.head.visible = true;
        hatModel.hat.visible = true;
        return hatModel;
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
