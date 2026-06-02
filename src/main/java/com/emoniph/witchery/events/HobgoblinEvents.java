package com.emoniph.witchery.events;

import com.emoniph.witchery.entity.HobgoblinEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber
public final class HobgoblinEvents {
    private HobgoblinEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHobgoblinIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof HobgoblinEntity
                && event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setCanceled(true);
        }
    }
}
