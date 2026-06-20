package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.BrewWarningMessages;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class BrewWarningEvents {

    private BrewWarningEvents() {}

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!event.isCanceled()) {
            BrewWarningMessages.remember(event.getPlayer(), event.getRawText());
        }
    }
}
