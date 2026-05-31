package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.item.WitchHandItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class WitchHandEvents {
    private WitchHandEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity target)
                || !WitchHandItem.isWitchHand(player.getMainHandItem())) {
            return;
        }

        if (WitchHandItem.leftClickEntity(player.getMainHandItem(), player, target)) {
            event.setCanceled(true);
            player.setLastHurtMob(target);
            player.onAttack();
        }
    }
}
