package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WitchHunterEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber
public final class VillageSocialEvents {
    private VillageSocialEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVillagerIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof VillageGuardEntity || attacker instanceof WitchHunterEntity) {
            event.setCanceled(true);
            return;
        }
        if (WitcheryConfig.TOWN_ZOMBIE_ATTACK_REDUCTION_MODE.get() >= 1
                && attacker instanceof Zombie) {
            event.setAmount(0.5F);
        }
    }
}
