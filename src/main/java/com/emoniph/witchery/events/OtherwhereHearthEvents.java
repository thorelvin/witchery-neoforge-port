package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ritual.OtherwhereHearthTransposition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class OtherwhereHearthEvents {
    private OtherwhereHearthEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(Level.OVERWORLD)
                && level.getGameTime() % 20L == 0L) {
            OtherwhereHearthTransposition.recover(level.getServer());
        }
    }
}
