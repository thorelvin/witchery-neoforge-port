package com.emoniph.witchery.events;

import com.emoniph.witchery.entity.familiar.CreakingFamiliarHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public final class CreakingFamiliarEvents {

    private CreakingFamiliarEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Creaking creaking)) {
            return;
        }
        if (!(creaking.level() instanceof ServerLevel serverLevel) || serverLevel.isClientSide()) {
            return;
        }
        CreakingFamiliarHelper.serverTick(serverLevel, creaking);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCreakingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Creaking creaking) || !CreakingFamiliarHelper.isBound(creaking)) {
            return;
        }

        if (creaking.isTearingDown()) {
            event.setCanceled(true);
            creaking.setHealth(creaking.getMaxHealth());
            creaking.setIsActive(true);
            return;
        }

        ServerPlayer owner = CreakingFamiliarHelper.findOwner(creaking);
        if (owner != null) {
            CreakingFamiliarHelper.markDismissed(owner, creaking, false);
            owner.hurt(owner.damageSources().magic(), owner.getMaxHealth() * 2.0F);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getTarget() instanceof Creaking creaking) || !CreakingFamiliarHelper.isBound(creaking)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !CreakingFamiliarHelper.isOwnedBy(creaking, player)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.isShiftKeyDown() && CreakingFamiliarHelper.isRootedGuard(creaking)) {
            CreakingFamiliarHelper.markDismissed(player, creaking, true);
        } else {
            CreakingFamiliarHelper.toggleMode(player, creaking);
        }
    }
}
