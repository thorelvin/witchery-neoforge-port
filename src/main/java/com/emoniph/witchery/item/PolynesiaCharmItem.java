package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class PolynesiaCharmItem extends Item {
    private final boolean charmsDemons;

    public PolynesiaCharmItem(Properties properties) {
        this(properties, false);
    }

    public PolynesiaCharmItem(Properties properties, boolean charmsDemons) {
        super(properties.durability(50).rarity(Rarity.UNCOMMON));
        this.charmsDemons = charmsDemons;
    }

    public boolean charmsDemons() {
        return charmsDemons;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String tooltipText = Component.translatable(getDescriptionId() + ".tip").getString();
        for (String line : tooltipText.split("\\n")) {
            if (!line.isEmpty()) {
                tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
