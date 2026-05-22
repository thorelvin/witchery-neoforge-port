package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public final class ParasyticLouseEvents {
    private static final Set<UUID> SELF_BITE_DAMAGE = new HashSet<>();

    private ParasyticLouseEvents() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getNewDamage() <= 0.0F || SELF_BITE_DAMAGE.contains(player.getUUID())) {
            return;
        }

        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(WitcheryItems.PARASYTIC_LOUSE.get())) {
                continue;
            }

            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (!contents.hasEffects()) {
                stack.remove(DataComponents.POTION_CONTENTS);
                continue;
            }

            LivingEntity target = chooseBiteTarget(contents, player, attacker);
            if (target == null) {
                continue;
            }

            contents.applyToLivingEntity(target, 1.0F);
            stack.remove(DataComponents.POTION_CONTENTS);
            player.getInventory().setChanged();
            applyLouseBiteCost(player);
            break;
        }
    }

    private static LivingEntity chooseBiteTarget(PotionContents contents, ServerPlayer player, LivingEntity attacker) {
        MobEffectInstance firstEffect = contents.getAllEffects().iterator().next();
        if (firstEffect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return attacker != null && attacker.isAlive() ? attacker : null;
        }
        return player;
    }

    private static void applyLouseBiteCost(ServerPlayer player) {
        SELF_BITE_DAMAGE.add(player.getUUID());
        try {
            player.hurt(player.damageSources().magic(), 1.0F);
        } finally {
            SELF_BITE_DAMAGE.remove(player.getUUID());
        }
    }
}
