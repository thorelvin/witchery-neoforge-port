package com.emoniph.witchery.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class WoodenScannerClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose SCANNER_HOLD =
            HumanoidModel.ArmPose.valueOf("WITCHERY_WOODEN_SCANNER_HOLD");

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack itemStack) {
        return SCANNER_HOLD;
    }
}
