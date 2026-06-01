package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.DeathsHandItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
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
        if (!held.is(WitcheryItems.DEATHS_HAND.get()) && !held.is(WitcheryItems.DEPLOYED_DEATHS_HAND.get())) return;

        event.setCanceled(true);
        DamageSource damageSource = level.damageSources().source(DEATHS_HAND_DAMAGE, player);
        if (DeathsHandItem.isDeployed(held)) {
            hitDeployedScythe(level, player, target, damageSource);
        } else {
            target.hurt(damageSource, calculateDamage(target, 0.1F, 2.0F));
        }

        player.setLastHurtMob(target);
        player.onAttack();
    }

    private static void hitDeployedScythe(ServerLevel level, ServerPlayer player, LivingEntity target, DamageSource damageSource) {
        HungerProfile profile = HungerProfile.forLevel(player.getFoodData().getFoodLevel());
        AABB area = target.getBoundingBox().inflate(1.5D, 0.0D, 1.5D);

        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player)) {
            float healthBefore = hit.getHealth();
            float absorptionBefore = hit.getAbsorptionAmount();
            hit.hurt(damageSource, calculateDamage(hit, profile.damagePct(), profile.minDamage()));
            boolean damaged = hit.getHealth() < healthBefore || hit.getAbsorptionAmount() < absorptionBefore || !hit.isAlive();
            if (damaged) {
                if (profile.hungerRestore() > 0) {
                    player.getFoodData().eat(profile.hungerRestore(), 0.0F);
                }
                if (profile.healthRestore() > 0) {
                    player.heal(profile.healthRestore());
                }
            }
        }
    }

    private static float calculateDamage(LivingEntity target, float damagePct, float minDamage) {
        float maxHealth = Math.min(target.getMaxHealth(), 20.0F);
        return Math.min(Math.max(maxHealth * damagePct, minDamage), 15.0F);
    }

    private record HungerProfile(float damagePct, float minDamage, int hungerRestore, int healthRestore) {
        private static HungerProfile forLevel(int hunger) {
            if (hunger == 0) {
                return new HungerProfile(0.5F, 4.0F, 10, 3);
            }
            if (hunger <= 4) {
                return new HungerProfile(0.25F, 4.0F, 3, 2);
            }
            if (hunger <= 10) {
                return new HungerProfile(0.2F, 3.0F, 2, 1);
            }
            return new HungerProfile(0.15F, 3.0F, hunger <= 20 ? 1 : 0, 0);
        }
    }
}
