package com.emoniph.witchery.events;

import com.emoniph.witchery.worldgen.ApothecaryVillager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public final class ApothecaryVillagerEvents {
    private ApothecaryVillagerEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Villager villager) {
            ApothecaryVillager.refreshIfApothecary(villager);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getTarget() instanceof Villager villager) {
            ApothecaryVillager.refreshIfApothecary(villager);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().tickCount % 40 != 0) {
            return;
        }
        if (event.getEntity() instanceof Villager villager) {
            ApothecaryVillager.refreshIfApothecary(villager);
        }
    }

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getAbstractVillager() instanceof Villager villager)) {
            return;
        }
        if (ApothecaryVillager.handleTrade(villager)) {
            ApothecaryVillager.syncOffers(villager, event.getEntity());
        }
    }
}
