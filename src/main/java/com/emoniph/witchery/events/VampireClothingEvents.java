package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.item.VampireClothingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public final class VampireClothingEvents {
    private VampireClothingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLethalFireDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (event.getNewDamage() <= 0.0F || !event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        if (!WitcheryAttachments.get(player).isVampire() || player.getHealth() - event.getNewDamage() > 0.0F) {
            return;
        }

        if (shouldSurviveFire(player)) {
            event.setNewDamage(Math.max(0.0F, player.getHealth() - 1.0F));
            player.clearFire();
        }
    }

    private static boolean shouldSurviveFire(ServerPlayer player) {
        if (VampireClothingUtil.isExtendedFlameProtectionActive(player)) {
            return player.getRandom().nextInt(4) != 0;
        }
        return VampireClothingUtil.isBasicFlameProtectionActive(player)
                && player.getRandom().nextInt(4) == 0;
    }
}
