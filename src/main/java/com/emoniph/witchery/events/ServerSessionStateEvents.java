package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AreaMarkerBlockEntity;
import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.brewing.BrewWarningMessages;
import com.emoniph.witchery.data.BedTaglockSavedData;
import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Clears transient process-global state before an integrated server can be replaced. */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class ServerSessionStateEvents {
    private ServerSessionStateEvents() {
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        FlyingMonkeyEntity.releasePreparationTickets(event.getServer());
        EarthShockwaveEvents.clearSession();
        CursedBlockEvents.clearSession();
        BrewWarningMessages.clearSession();
        BedTaglockSavedData.clearSession();
        AreaMarkerBlockEntity.clearServer(event.getServer());
        PoppetShelfBlockEntity.clearServer(event.getServer());
    }
}
