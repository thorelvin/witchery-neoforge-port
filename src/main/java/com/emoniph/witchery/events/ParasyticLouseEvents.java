package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        if (tryUseHotbarLouse(player, attacker)) {
            return;
        }
        tryUseBitingBelt(player, attacker);
    }

    private static boolean tryUseHotbarLouse(ServerPlayer player, LivingEntity attacker) {
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
            applyBiteCost(player);
            return true;
        }
        return false;
    }

    private static LivingEntity chooseBiteTarget(PotionContents contents, ServerPlayer player, LivingEntity attacker) {
        MobEffectInstance firstEffect = contents.getAllEffects().iterator().next();
        if (firstEffect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return attacker != null && attacker.isAlive() ? attacker : null;
        }
        return player;
    }

    private static void tryUseBitingBelt(ServerPlayer player, LivingEntity attacker) {
        ItemStack belt = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!belt.is(WitcheryItems.BITING_BELT.get())) {
            return;
        }

        PotionContents contents = belt.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!contents.hasEffects()) {
            belt.remove(DataComponents.POTION_CONTENTS);
            return;
        }

        MobEffectInstance selected = null;
        List<MobEffectInstance> remaining = new ArrayList<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            MobEffectInstance copy = new MobEffectInstance(effect);
            if (selected == null && canBitingBeltUse(player, copy)) {
                selected = copy;
            } else {
                remaining.add(copy);
            }
        }

        if (selected == null) {
            return;
        }

        chooseBitingBeltTarget(selected, player, attacker).addEffect(new MobEffectInstance(selected));
        setBitingBeltEffects(belt, remaining);
        applyBiteCost(player);
    }

    private static boolean canBitingBeltUse(ServerPlayer player, MobEffectInstance effect) {
        return !player.hasEffect(effect.getEffect()) && !isBeneficialInstantEffect(effect);
    }

    private static boolean isBeneficialInstantEffect(MobEffectInstance effect) {
        return effect.getEffect().value().isInstantenous()
                && effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
    }

    private static LivingEntity chooseBitingBeltTarget(MobEffectInstance effect, ServerPlayer player, LivingEntity attacker) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && attacker != null && attacker.isAlive()) {
            return attacker;
        }
        return player;
    }

    private static void setBitingBeltEffects(ItemStack belt, List<MobEffectInstance> effects) {
        if (effects.isEmpty()) {
            belt.remove(DataComponents.POTION_CONTENTS);
        } else {
            PotionContents contents = PotionContents.EMPTY;
            for (MobEffectInstance effect : effects) {
                contents = contents.withEffectAdded(new MobEffectInstance(effect));
            }
            belt.set(DataComponents.POTION_CONTENTS, contents);
        }
    }

    private static void applyBiteCost(ServerPlayer player) {
        SELF_BITE_DAMAGE.add(player.getUUID());
        try {
            player.hurt(player.damageSources().magic(), 1.0F);
        } finally {
            SELF_BITE_DAMAGE.remove(player.getUUID());
        }
    }
}
