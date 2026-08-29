package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.BlightHeartSavedData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Runs bounded, loaded-chunk-only restoration for destroyed or cleansed Blight Hearts. */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class BlightHeartEvents {
    private static final int CLEANUP_INTERVAL_TICKS = 10;

    private BlightHeartEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.getGameTime() % CLEANUP_INTERVAL_TICKS == 0L) {
            BlightHeartSavedData.get(level.getServer()).tickLevel(level);
        }
    }
}
