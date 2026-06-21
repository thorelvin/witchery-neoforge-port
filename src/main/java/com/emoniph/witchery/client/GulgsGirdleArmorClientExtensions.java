package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.GulgsGirdleArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class GulgsGirdleArmorClientExtensions implements IClientItemExtensions {
    private GulgsGirdleArmorModel model;

    private GulgsGirdleArmorModel model() {
        if (model == null) {
            model = new GulgsGirdleArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(
                    GulgsGirdleArmorModel.LAYER_LOCATION));
        }
        return model;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        GulgsGirdleArmorModel girdleModel = model();
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, girdleModel.head);
            copyPose(originalHumanoid.hat, girdleModel.hat);
            copyPose(originalHumanoid.body, girdleModel.body);
            copyPose(originalHumanoid.rightArm, girdleModel.rightArm);
            copyPose(originalHumanoid.leftArm, girdleModel.leftArm);
            copyPose(originalHumanoid.rightLeg, girdleModel.rightLeg);
            copyPose(originalHumanoid.leftLeg, girdleModel.leftLeg);
        }
        girdleModel.setAllVisible(false);
        girdleModel.body.visible = true;
        girdleModel.rightLeg.visible = true;
        girdleModel.leftLeg.visible = true;
        return girdleModel;
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
