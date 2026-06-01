package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "removal"})
public class BabasHatItem extends Item {
    public BabasHatItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.babas_hat.tip.1")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.witchery.babas_hat.tip.2")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.empty());
        tooltip.accept(Component.translatable("item.witchery.babas_hat.tip.brew2")
                .withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("item.witchery.babas_hat.tip.brew3")
                .withStyle(ChatFormatting.BLUE));
    }
}
