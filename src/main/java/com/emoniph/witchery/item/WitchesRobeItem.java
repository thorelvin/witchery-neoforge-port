package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "removal"})
public class WitchesRobeItem extends Item {
    public WitchesRobeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".tip.1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(getDescriptionId() + ".tip.2")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".tip.3")
                .withStyle(ChatFormatting.BLUE));
    }
}
