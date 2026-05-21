package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class MirrorWorldEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!WitcheryDimensions.isMirrorWorld(level)) return;

        if (!level.isRaining() && !level.isThundering()) return;

        // Mirror World should stay unnaturally still rather than inheriting Overworld storms.
        level.setWeatherParameters(6000, 0, false, false);
    }
}
