package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.effect.EnderInhibitionMobEffect;
import com.emoniph.witchery.effect.PoisonWeaponsMobEffect;
import com.emoniph.witchery.effect.RepellAttackerMobEffect;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class BrewMobEffectEvents {
    private BrewMobEffectEvents() {
    }

    @SubscribeEvent
    public static void onRepellAttackerDamage(LivingDamageEvent.Pre event) {
        LivingEntity protectedEntity = event.getEntity();
        if (protectedEntity.level().isClientSide()) {
            return;
        }
        MobEffectInstance repell = protectedEntity.getEffect(WitcheryMobEffects.REPELL_ATTACKER);
        if (repell == null) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                || event.getSource().getDirectEntity() instanceof Projectile) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)
                || attacker == protectedEntity
                || attacker.distanceToSqr(protectedEntity) >= RepellAttackerMobEffect.MAX_RANGE_SQ) {
            return;
        }

        Vec3 motion = RepellAttackerMobEffect.pushbackMotion(attacker, protectedEntity, repell.getAmplifier());
        attacker.setDeltaMovement(motion);
        attacker.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onPoisonWeaponsDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || !PoisonWeaponsMobEffect.isSourceMeleeDamage(event.getSource())) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        MobEffectInstance poisonWeapons = attacker.getEffect(WitcheryMobEffects.POISON_WEAPONS);
        if (poisonWeapons == null) {
            return;
        }

        target.addEffect(PoisonWeaponsMobEffect.attackEffect(poisonWeapons.getAmplifier()));
    }

    @SubscribeEvent
    public static void onEnderEntityTeleport(EntityTeleportEvent.EnderEntity event) {
        cancelIfEnderInhibited(event, event.getEntityLiving());
    }

    @SubscribeEvent
    public static void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        cancelIfEnderInhibited(event, event.getPlayer());
    }

    private static void cancelIfEnderInhibited(EntityTeleportEvent event, LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(WitcheryMobEffects.ENDER_INHIBITION);
        if (effect != null && !EnderInhibitionMobEffect.canTeleport(effect.getAmplifier(), 0)) {
            event.setCanceled(true);
        }
    }
}
