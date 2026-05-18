package com.emoniph.witchery.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Vampiric Poppet — offensive. While in the holder's inventory, redirects 50% of incoming
 * damage to the bound player. Consumed on first activation.
 * Activation is handled by PoppetEvents.onLivingDamage.
 */
public class VampiricPoppetItem extends PoppetItem {

    public VampiricPoppetItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, _display, tooltip, flag);
        tooltip.accept(Component.translatable("witchery.poppet.vampiric.tooltip")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
