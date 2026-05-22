package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class EarmuffsItem extends Item {
    public EarmuffsItem(Properties properties) {
        super(properties);
    }

    public static boolean isWorn(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.EARMUFFS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                net.minecraft.world.item.component.TooltipDisplay _display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.earmuffs.tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
