package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.AnchoredCurseSavedData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Keeps Red Thread curses bounded to loaded anchor chunks. */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class AnchoredCurseEvents {
    private AnchoredCurseEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.getGameTime() % AnchoredCurseSavedData.TICK_INTERVAL == 0L) {
            AnchoredCurseSavedData.get(level.getServer()).tickLevel(level);
        }
    }
}
