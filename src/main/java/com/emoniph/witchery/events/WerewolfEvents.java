package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.emoniph.witchery.entity.WereVillagerEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles all werewolf gameplay: transformation buffs, wolfsbane weakness, and silver damage bonus.
 *
 * Wolf form buffs apply only while the werewolf transformation state is active.
 * Carrying a Moon Charm suppresses automatic moon-forced shifting.
 * Wolfsbane in inventory forces the transformation off and applies Weakness II.
 * Silver ingots/weapons deal bonus damage to werewolves and vampires.
 */
@EventBusSubscriber
public class WerewolfEvents {

    private static final int EFFECT_INTERVAL = 100; // ticks between effect refresh
    private static final int EFFECT_DURATION = 140;
    private static final int SYNC_INTERVAL   = 200;
    private static final float VILLAGER_INFECTION_CHANCE = 0.10F;
    private static final float WOLFMAN_INFECTION_HEALTH_FRACTION = 0.25F;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isWerewolf()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        int tick = player.tickCount;
        if (tick % 40              == 0) updateMoonShift(player, level, data);
        if (tick % EFFECT_INTERVAL == 0) applyWolfEffects(player, data);
        if (data.getHowlCooldown() > 0) data.setHowlCooldown(data.getHowlCooldown() - 1);
        if (tick % SYNC_INTERVAL   == 0) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void updateMoonShift(ServerPlayer player, ServerLevel level, WitcheryPlayerData data) {
        if (WerewolfShiftUtil.hasWolfsbane(player)) {
            if (WerewolfShiftUtil.isTransformed(data)) {
                WerewolfShiftUtil.setTransformed(player, data, false,
                        Component.translatable("witchery.werewolf.wolfsbane"));
            }
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, true));
            player.removeEffect(MobEffects.SPEED);
            player.removeEffect(MobEffects.STRENGTH);
            player.removeEffect(MobEffects.JUMP_BOOST);
            if (player.tickCount % 200 == 0) {
                player.displayClientMessage(Component.translatable("witchery.werewolf.wolfsbane"), false);
            }
            return;
        }

        if (WerewolfShiftUtil.hasMoonCharm(player)) {
            return;
        }

        boolean shouldTransform = !WitcheryConfig.WEREWOLF_FULL_MOON_ONLY.get() || isFullMoon(level);
        if (shouldTransform != WerewolfShiftUtil.isTransformed(data)) {
            WerewolfShiftUtil.setTransformed(player, data, shouldTransform,
                    shouldTransform ? Component.translatable("witchery.werewolf.transformed")
                            : Component.translatable("witchery.werewolf.restored"));
        }
    }

    private static void applyWolfEffects(ServerPlayer player, WitcheryPlayerData data) {
        if (WerewolfShiftUtil.isTransformed(data)) {
            applyWolfForm(player, data);
        }
    }

    // Silver damage bonus is now handled centrally in SilverEvents.onSilverDamage

    @SubscribeEvent
    public static void onInfectiousWolfmanDamage(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!(event.getSource().getEntity() instanceof WolfmanEntity wolfman)) return;
        if (!wolfman.isInfectious()) return;
        if (!(villager.level() instanceof ServerLevel level)) return;

        float healthAfterDamage = villager.getHealth() - event.getNewDamage();
        if (healthAfterDamage >= villager.getMaxHealth() * WOLFMAN_INFECTION_HEALTH_FRACTION) return;

        if (spawnWereVillager(level, villager)) {
            event.setNewDamage(0.0F);
            villager.discard();
        }
    }

    @SubscribeEvent
    public static void onWerewolfVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!WitcheryAttachments.get(player).isWerewolf()) return;
        if (!isFullMoonNight(level)) return;
        if (level.random.nextFloat() >= VILLAGER_INFECTION_CHANCE) return;

        if (spawnWereVillager(level, villager)) {
            event.setCanceled(true);
            villager.discard();
        }
    }

    private static boolean isSilverHeld(ItemStack stack) {
        return stack.is(WitcheryItems.SILVER_INGOT.get())
                || stack.is(WitcheryItems.SILVER_SWORD.get());
    }

    private static boolean isWerewolfOrVampire(LivingEntity entity) {
        if (entity instanceof ServerPlayer target) {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            return data.isWerewolf() || data.isVampire();
        }
        return entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    private static boolean spawnWereVillager(ServerLevel level, Villager villager) {
        WereVillagerEntity were = WitcheryEntities.WERE_VILLAGER.get()
                .create(level, EntitySpawnReason.TRIGGERED);
        if (were == null) return false;
        were.setPos(villager.getX(), villager.getY(), villager.getZ());
        were.setYRot(villager.getYRot());
        were.setXRot(villager.getXRot());
        were.setFormerProfession(0); // profession stored as type (0=default); full profession mapping deferred
        were.setInfectious(true); // player-caused infection is always infectious
        were.setPersistenceRequired();
        if (villager.hasCustomName()) were.setCustomName(villager.getCustomName());
        level.addFreshEntity(were);
        level.levelEvent(null, 1017, were.blockPosition(), 0);
        return true;
    }

    private static void applyWolfForm(ServerPlayer player, WitcheryPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, 2, true, true));  // Speed III
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,   EFFECT_DURATION, 1, true, true));  // Strength II
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,           EFFECT_DURATION, 1, true, true));  // Jump Boost II
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER,         EFFECT_DURATION, 1, true, true));  // Hunger II
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,   EFFECT_DURATION, 0, true, false)); // Night Vision

    }

    /** Phase 0 = full moon in vanilla's 8-phase moon cycle. */
    private static boolean isFullMoon(ServerLevel level) {
        return isFullMoon(level.getDayTime());
    }

    /** Full moon when the day count (dayTime / 24000) lands on moon phase 0. */
    static boolean isFullMoon(long dayTime) {
        return (dayTime / 24000L) % 8L == 0;
    }

    private static boolean isFullMoonNight(ServerLevel level) {
        return isFullMoonNight(level.getDayTime());
    }

    /** A full-moon night: full-moon day and the time-of-day is within the night window (13000–23000). */
    static boolean isFullMoonNight(long dayTime) {
        long timeOfDay = dayTime % 24000L;
        return isFullMoon(dayTime) && timeOfDay > 13000L && timeOfDay < 23000L;
    }
}
