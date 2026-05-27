package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber
public final class DeathsHandEvents {
    private static final ResourceKey<DamageType> DEATHS_HAND_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "deaths_hand"));

    private DeathsHandEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(WitcheryItems.DEATHS_HAND.get())) return;

        float maxHealth = Math.min(target.getMaxHealth(), 20.0F);
        float damage = Math.min(Math.max(maxHealth * 0.1F, 2.0F), 15.0F);

        event.setCanceled(true);
        target.hurt(level.damageSources().source(DEATHS_HAND_DAMAGE, player), damage);
        player.setLastHurtMob(target);
        player.onAttack();
    }
}
