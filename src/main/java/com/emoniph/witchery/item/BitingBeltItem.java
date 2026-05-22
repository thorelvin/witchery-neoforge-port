package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.function.Consumer;

public class BitingBeltItem extends Item {
    public BitingBeltItem(Properties properties) {
        super(properties);
    }

    public static boolean isWorn(Player player) {
        return player.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.BITING_BELT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.biting_belt.tip").withStyle(ChatFormatting.GRAY));
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            PotionContents.addPotionTooltip(contents.getAllEffects(), tooltip, 1.0F, context.tickRate());
        }
    }
}
