package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.CaneSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class CaneSwordEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(WitcheryItems.CANE_SWORD.get())
                && !event.getItemStack().is(WitcheryItems.DRAWN_CANE_SWORD.get())) {
            return;
        }
        if (event.getEntity() == null) {
            return;
        }
        int reserve = WitcheryAttachments.get(event.getEntity()).getBloodReserve();
        event.getToolTip().add(Component.translatable("item.witchery.cane_sword.tip.reserve",
                reserve, CaneSwordItem.MAX_BLOOD_RESERVE).withStyle(ChatFormatting.DARK_RED));
    }
}
