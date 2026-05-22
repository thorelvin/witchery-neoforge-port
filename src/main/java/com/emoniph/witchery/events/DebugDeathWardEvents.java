package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public final class DebugDeathWardEvents {
    private static final float MIN_SURVIVAL_HEALTH = 2.0F;

    private DebugDeathWardEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getNewDamage() <= 0.0F || !hasDeathWard(player.getInventory())) {
            return;
        }

        float maxAllowedDamage = player.getHealth() - MIN_SURVIVAL_HEALTH;
        if (maxAllowedDamage <= 0.0F) {
            event.setNewDamage(0.0F);
            return;
        }

        if (event.getNewDamage() > maxAllowedDamage) {
            event.setNewDamage(maxAllowedDamage);
        }
    }

    private static boolean hasDeathWard(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(WitcheryItems.DEBUG_DEATH_WARD.get())) {
                return true;
            }
        }
        return false;
    }
}
