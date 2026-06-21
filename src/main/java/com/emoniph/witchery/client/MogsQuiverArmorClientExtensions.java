package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.MogsQuiverArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class MogsQuiverArmorClientExtensions implements IClientItemExtensions {
    private MogsQuiverArmorModel model;

    private MogsQuiverArmorModel model() {
        if (model == null) {
            model = new MogsQuiverArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    MogsQuiverArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        MogsQuiverArmorModel quiverModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, quiverModel.head);
            copyPose(originalHumanoid.hat, quiverModel.hat);
            copyPose(originalHumanoid.body, quiverModel.body);
            copyPose(originalHumanoid.rightArm, quiverModel.rightArm);
            copyPose(originalHumanoid.leftArm, quiverModel.leftArm);
            copyPose(originalHumanoid.rightLeg, quiverModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, quiverModel.leftLeg);
        }
        quiverModel.setAllVisible(false);
        quiverModel.body.visible = true;
        quiverModel.rightArm.visible = true;
        quiverModel.leftArm.visible = true;
        return quiverModel;
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
