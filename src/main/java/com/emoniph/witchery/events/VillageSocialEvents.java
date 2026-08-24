package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WitchHunterEntity;
import com.emoniph.witchery.util.VillagerBlood;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber
public final class VillageSocialEvents {
    private static final double GUARD_ALERT_RADIUS = 16.0D;
    private static final Map<Villager, Integer> SLEEP_TICKS = new WeakHashMap<>();

    private VillageSocialEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVillagerIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Player villageFriend && attacker instanceof Player playerAttacker) {
            alertGuardsDefendingVillageFriend(villageFriend, playerAttacker);
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (attacker instanceof VillageGuardEntity || attacker instanceof WitchHunterEntity) {
            event.setCanceled(true);
            return;
        }
        if (attacker instanceof LivingEntity livingAttacker) {
            alertVillageGuards(villager, livingAttacker);
        }
        if (WitcheryConfig.TOWN_ZOMBIE_ATTACK_REDUCTION_MODE.get() >= 1
                && attacker instanceof Zombie) {
            event.setAmount(0.5F);
        }
    }

    @SubscribeEvent
    public static void onVillagerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide()) {
            return;
        }
        if (villager.isSleeping()) {
            SLEEP_TICKS.merge(villager, 1, Integer::sum);
            return;
        }
        Integer sleepingTicks = SLEEP_TICKS.remove(villager);
        if (sleepingTicks != null) {
            VillagerBlood.recoverAfterSleep(villager, sleepingTicks);
        }
    }

    static void alertVillageGuards(Villager victim, LivingEntity attacker) {
        AABB bounds = victim.getBoundingBox().inflate(GUARD_ALERT_RADIUS, 8.0D, GUARD_ALERT_RADIUS);
        for (VillageGuardEntity guard : victim.level().getEntitiesOfClass(VillageGuardEntity.class, bounds)) {
            if (guard.canDefendVillageAgainst(attacker)) {
                guard.setTarget(attacker);
            }
        }
    }

    static int alertGuardsDefendingVillageFriend(Player victim, Player attacker) {
        AABB bounds = victim.getBoundingBox().inflate(32.0D, 16.0D, 32.0D);
        int alerted = 0;
        for (VillageGuardEntity guard : victim.level().getEntitiesOfClass(VillageGuardEntity.class, bounds)) {
            if (guard.protectsVillageFriend(victim)) {
                guard.setTarget(attacker);
                alerted++;
            }
        }
        return alerted;
    }
}
