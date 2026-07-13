package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AreaMarkerBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class AreaMarkerEvents {
    private AreaMarkerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerKilledByPlayer(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) {
            return;
        }
        AreaMarkerBlockEntity.markKillerNear(killer);
    }
}
