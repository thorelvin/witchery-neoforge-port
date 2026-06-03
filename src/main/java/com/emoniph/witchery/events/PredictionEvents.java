package com.emoniph.witchery.events;

import com.emoniph.witchery.predictions.Predictions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Drives crystal-ball prophecy fulfillment: a stored prediction comes true naturally (the player
 * actually gets hit by an arrow, falls, gets wet, or mines the foretold block) or — once past-due —
 * is force-fulfilled from the player tick.
 */
@EventBusSubscriber
public class PredictionEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getGameTime() % 20L == 0L) {
            Predictions.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Predictions.onDamage(player, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
            Predictions.onMine(player, level, event.getPos(), event.getState());
        }
    }
}
