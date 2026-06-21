package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class InfusionSourceItem extends Item {
    private final String infusionNameKey;

    public InfusionSourceItem(Properties properties, String infusionNameKey) {
        super(properties);
        this.infusionNameKey = infusionNameKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.infusion_source.tip",
                        Component.translatable(infusionNameKey))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.witchery.infusion_source.rite")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.infusion_source.kettle")
                .withStyle(ChatFormatting.GRAY));
    }
}
