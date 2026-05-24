package com.emoniph.witchery.item;

import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AppleOfSleepingItem extends Item {

    public AppleOfSleepingItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(3).saturationModifier(3.0f).alwaysEdible().build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            SpiritWorldHelper.enter(player);
        }
        return result;
    }
}
