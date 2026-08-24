package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.effect.ChilledMobEffect;
import com.emoniph.witchery.effect.DiseasedMobEffect;
import com.emoniph.witchery.effect.EnderInhibitionMobEffect;
import com.emoniph.witchery.effect.MoonshineMobEffect;
import com.emoniph.witchery.effect.ParalyzedMobEffect;
import com.emoniph.witchery.effect.PoisonWeaponsMobEffect;
import com.emoniph.witchery.effect.RepellAttackerMobEffect;
import com.emoniph.witchery.effect.ResizingMobEffect;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class BrewMobEffectEvents {
    private static final ThreadLocal<Boolean> REFLECTING_DAMAGE = ThreadLocal.withInitial(() -> false);

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMoonshineDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide()
                || !MoonshineMobEffect.isSourcePainDamage(event.getSource())) {
            return;
        }
        MobEffectInstance moonshine = player.getEffect(WitcheryMobEffects.MOONSHINE);
        if (moonshine == null) {
            return;
        }
        int foodPenalty = MoonshineMobEffect.foodPenalty(event.getNewDamage(), moonshine.getAmplifier(),
                player.getFoodData().getFoodLevel());
        if (foodPenalty <= 0) {
            return;
        }

        player.getFoodData().eat(-foodPenalty, 2.0F);
        event.setNewDamage(0.0F);
    }

    @SubscribeEvent
    public static void onChilledFireDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !ChilledMobEffect.isSourceFireDamage(event.getSource())) {
            return;
        }
        MobEffectInstance chilled = entity.getEffect(WitcheryMobEffects.CHILLED);
        if (chilled != null) {
            event.setNewDamage(ChilledMobEffect.fireDamageAfterReduction(event.getNewDamage(), chilled.getAmplifier()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCustomBrewDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }

        MobEffectInstance vines = entity.getEffect(WitcheryMobEffects.WRAPPED_IN_VINES);
        if (vines != null && isMagicDamage(event.getSource())) {
            event.setNewDamage(event.getNewDamage() * sourceWrappedMagicMultiplier(vines.getAmplifier()));
        }

        MobEffectInstance absorb = entity.getEffect(WitcheryMobEffects.ABSORB_MAGIC);
        if (absorb != null && isMagicDamage(event.getSource())) {
            float absorbed = event.getNewDamage() * sourceAbsorbMagicFraction(absorb.getAmplifier());
            event.setNewDamage(Math.max(0.0F, event.getNewDamage() - absorbed));
            if (entity instanceof Player player && absorbed > 1.0F) {
                var data = WitcheryAttachments.get(player);
                int maximum = data.getInfusionMaxEnergy();
                if (maximum > 0) {
                    data.setInfusionEnergy(Math.min(maximum,
                            data.getInfusionEnergy() + (int) absorbed), maximum);
                }
            }
        }

        applyResizingDamage(event);
        reflectDamage(event);
        triggerVolatility(event);
    }

    private static void applyResizingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker == event.getEntity()
                || !event.getSource().isDirect()) {
            return;
        }
        MobEffectInstance attackerSize = attacker.getEffect(WitcheryMobEffects.RESIZING);
        MobEffectInstance defenderSize = event.getEntity().getEffect(WitcheryMobEffects.RESIZING);
        if (attackerSize != null || defenderSize != null) {
            event.setNewDamage(event.getNewDamage()
                    * ResizingMobEffect.sourceMeleeDamageMultiplier(attackerSize, defenderSize));
        }
    }

    private static void reflectDamage(LivingDamageEvent.Pre event) {
        if (REFLECTING_DAMAGE.get()) {
            return;
        }
        MobEffectInstance reflection = event.getEntity().getEffect(WitcheryMobEffects.REFLECT_DAMAGE);
        if (reflection == null || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker == event.getEntity()
                || event.getSource().is(DamageTypeTags.IS_PROJECTILE) && reflection.getAmplifier() < 2) {
            return;
        }
        float reflected = sourceReflectedDamage(event.getNewDamage(), reflection.getAmplifier(),
                event.getSource().is(DamageTypeTags.IS_PROJECTILE));
        if (reflected <= 0.0F) {
            return;
        }
        event.setNewDamage(Math.max(0.0F, event.getNewDamage() - reflected));
        try {
            REFLECTING_DAMAGE.set(true);
            attacker.hurt(event.getSource(), reflected);
        } finally {
            REFLECTING_DAMAGE.set(false);
        }
    }

    private static void triggerVolatility(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance volatility = entity.getEffect(WitcheryMobEffects.VOLATILITY);
        if (volatility == null || !isExplodableDamage(event.getSource())) {
            return;
        }
        int amplifier = volatility.getAmplifier();
        boolean sourceExplosion = event.getSource().is(DamageTypeTags.IS_EXPLOSION);
        if (!sourceExplosion && entity.getRandom().nextInt(5 - Math.min(amplifier, 3)) != 0) {
            return;
        }
        if (entity.getRandom().nextInt(amplifier + 3) == 0) {
            entity.removeEffect(WitcheryMobEffects.VOLATILITY);
        }
        entity.level().explode(sourceExplosion ? entity : null,
                entity.getX(), entity.getY(), entity.getZ(),
                sourceVolatilityRadius(amplifier), Level.ExplosionInteraction.MOB);
    }

    static float sourceWrappedMagicMultiplier(int amplifier) {
        return Math.min(Math.max(0, amplifier) + 1, 4);
    }

    static float sourceAbsorbMagicFraction(int amplifier) {
        return 0.2F * (Math.max(0, amplifier) + 1);
    }

    static float sourceReflectedDamage(float damage, int amplifier, boolean projectile) {
        if (damage <= 0.0F || projectile && amplifier < 2) {
            return 0.0F;
        }
        int factor = Math.max(0, amplifier) + (projectile ? 0 : 1);
        return (float) Math.ceil(damage * 0.1F * factor);
    }

    static float sourceVolatilityRadius(int amplifier) {
        return Math.min(2.0F + 0.5F * Math.max(0, amplifier), 3.0F);
    }

    private static boolean isMagicDamage(DamageSource source) {
        return source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private static boolean isExplodableDamage(DamageSource source) {
        return !source.is(DamageTypes.IN_WALL)
                && !source.is(DamageTypes.DROWN)
                && !source.is(DamageTypes.FALL)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.STARVE);
    }

    @SubscribeEvent
    public static void onReincarnateDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        MobEffectInstance reincarnate = dead.getEffect(WitcheryMobEffects.REINCARNATE);
        if (reincarnate == null) {
            return;
        }
        EntityType<? extends Mob> type = reincarnationType(dead, reincarnate.getAmplifier());
        Mob reborn = type.create(level, EntitySpawnReason.TRIGGERED);
        if (reborn == null) {
            return;
        }
        reborn.snapTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), 0.0F);
        reborn.finalizeSpawn(level, level.getCurrentDifficultyAt(dead.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            reborn.setTarget(attacker);
        }
        level.addFreshEntity(reborn);
    }

    static EntityType<? extends Mob> reincarnationType(LivingEntity entity, int amplifier) {
        boolean animalTier = entity instanceof Animal || entity instanceof Spider;
        if (animalTier) {
            return amplifier > 2 ? EntityType.CREEPER
                    : amplifier > 1 ? EntityType.CAVE_SPIDER : EntityType.SPIDER;
        }
        return amplifier > 2 ? EntityType.BLAZE
                : amplifier > 1 ? EntityType.SKELETON : EntityType.ZOMBIE;
    }

    @SubscribeEvent
    public static void onResizingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance resizing = entity.getEffect(WitcheryMobEffects.RESIZING);
        if (resizing == null) {
            return;
        }
        Vec3 velocity = entity.getDeltaMovement();
        entity.setDeltaMovement(velocity.x,
                velocity.y * ResizingMobEffect.sourceJumpMultiplier(resizing.getAmplifier()), velocity.z);
        entity.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onParalysedDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance paralysis = entity.getEffect(WitcheryMobEffects.PARALYSED);
        if (paralysis != null && ParalyzedMobEffect.shouldBreakOnHurt(entity.level().isClientSide(),
                paralysis.getAmplifier(), event.getNewDamage())) {
            entity.removeEffect(WitcheryMobEffects.PARALYSED);
        }
    }

    @SubscribeEvent
    public static void onParalysedAdded(MobEffectEvent.Added event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect.is(WitcheryMobEffects.PARALYSED)) {
            ParalyzedMobEffect.syncSourceSpeedModifier(event.getEntity(), effect.getAmplifier());
        } else if (effect.is(WitcheryMobEffects.DISEASED)) {
            DiseasedMobEffect.syncSourceAttackDamageModifier(event.getEntity(), effect.getAmplifier());
        } else if (effect.is(WitcheryMobEffects.RESIZING)) {
            ResizingMobEffect.syncScale(event.getEntity(), effect.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onParalysedRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().is(WitcheryMobEffects.PARALYSED)) {
            ParalyzedMobEffect.removeSourceSpeedModifier(event.getEntity());
        } else if (event.getEffect().is(WitcheryMobEffects.DISEASED)) {
            DiseasedMobEffect.removeSourceAttackDamageModifier(event.getEntity());
        } else if (event.getEffect().is(WitcheryMobEffects.RESIZING)) {
            ResizingMobEffect.removeScale(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onParalysedExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && effect.is(WitcheryMobEffects.PARALYSED)) {
            LivingEntity entity = event.getEntity();
            ParalyzedMobEffect.removeSourceSpeedModifier(entity);
            if (ParalyzedMobEffect.shouldApplyQueasyOnExpiry(entity.level().isClientSide(),
                    entity instanceof Player, effect.getAmplifier())) {
                entity.addEffect(new MobEffectInstance(WitcheryMobEffects.QUEASY,
                        ParalyzedMobEffect.QUEASY_EXPIRY_DURATION_TICKS, 0, true, true));
            }
        } else if (effect != null && effect.is(WitcheryMobEffects.DISEASED)) {
            DiseasedMobEffect.removeSourceAttackDamageModifier(event.getEntity());
        } else if (effect != null && effect.is(WitcheryMobEffects.RESIZING)) {
            ResizingMobEffect.removeScale(event.getEntity());
        }
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
