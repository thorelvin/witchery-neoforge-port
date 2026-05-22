package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public class VampireClothingItem extends Item {
    private final SetType setType;

    public VampireClothingItem(SetType setType, Properties properties) {
        super(properties);
        this.setType = setType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.vampire_clothing.tooltip.header")
                .withStyle(ChatFormatting.DARK_RED));
        String prefix = "item.witchery.vampire_clothing.tooltip." + setType.langKey + ".";
        for (int i = 1; i <= setType.lines; i++) {
            tooltip.accept(Component.translatable(prefix + i).withStyle(ChatFormatting.GRAY));
        }
    }

    public enum SetType {
        LIGHT("light", 4),
        MEDIUM("medium", 1);

        private final String langKey;
        private final int lines;

        SetType(String langKey, int lines) {
            this.langKey = langKey;
            this.lines = lines;
        }
    }
}
