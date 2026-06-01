package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public class HunterClothingItem extends Item {
    public enum Variant {
        PLAIN,
        SILVERED,
        DAWN
    }

    private final Variant variant;

    public HunterClothingItem(Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        if (variant == Variant.DAWN) {
            tooltip.accept(Component.translatable("item.witchery.hunter.tip.vampire")
                    .withStyle(ChatFormatting.BLUE));
        }
        if (variant == Variant.SILVERED || variant == Variant.DAWN) {
            tooltip.accept(Component.translatable("item.witchery.hunter.tip.werewolf")
                    .withStyle(ChatFormatting.BLUE));
        }
        tooltip.accept(Component.translatable("item.witchery.hunter.tip.curses")
                .withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("item.witchery.hunter.tip.bolts")
                .withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("item.witchery.hunter.tip.no_poppets")
                .withStyle(ChatFormatting.BLUE));
    }
}
