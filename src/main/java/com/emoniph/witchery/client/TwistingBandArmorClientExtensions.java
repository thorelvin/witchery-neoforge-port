package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.TwistingBandArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class TwistingBandArmorClientExtensions implements IClientItemExtensions {
    private TwistingBandArmorModel model;

    private TwistingBandArmorModel model() {
        if (model == null) {
            model = new TwistingBandArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    TwistingBandArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        TwistingBandArmorModel bandModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, bandModel.head);
            copyPose(originalHumanoid.hat, bandModel.hat);
            copyPose(originalHumanoid.body, bandModel.body);
            copyPose(originalHumanoid.rightArm, bandModel.rightArm);
            copyPose(originalHumanoid.leftArm, bandModel.leftArm);
            copyPose(originalHumanoid.rightLeg, bandModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, bandModel.leftLeg);
        }
        bandModel.setAllVisible(false);
        bandModel.head.visible = true;
        bandModel.hat.visible = true;
        return bandModel;
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
