package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfQuestState;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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

    /**
     * Wolf-Altar quest counters fed by combat (source GenericEvents): L4 Horned Huntsman defeat, L5
     * airborne hostile kill, L8 wolfman pigman kill, L9 wolf villager/player kill. Each only counts while
     * the matching quest is STARTED. <b>Phase 2 simplification:</b> the wolf-vs-wolfman form requirement
     * is reduced to "transformed" until form selection lands in Phase 3.
     */
    @SubscribeEvent
    public static void onWerewolfQuestKill(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isWerewolf() || data.getWolfQuestState() != WolfQuestState.STARTED) return;

        int wolfLevel = data.getWerewolfLevel();
        LivingEntity victim = event.getEntity();

        // L4→5: defeating the Horned Huntsman completes the Horn quest (no transform requirement).
        if (wolfLevel == 4 && victim instanceof HornedHuntsmanEntity) {
            data.setWolfQuestState(WolfQuestState.COMPLETE);
            sync(player, data);
            player.displayClientMessage(Component.translatable("witchery.werewolf.level5complete"), true);
            return;
        }

        if (!WerewolfShiftUtil.isTransformed(data)) return;
        boolean counted = switch (wolfLevel) {
            case 5 -> victim instanceof Enemy && !player.onGround();      // airborne hostile kill
            case 8 -> victim instanceof ZombifiedPiglin;
            case 9 -> victim instanceof Villager || victim instanceof Player;
            default -> false;
        };
        if (counted) {
            data.incrementWolfQuestCounter();
            sync(player, data);
            player.displayClientMessage(Component.translatable(
                    "witchery.werewolf.level" + (wolfLevel + 1) + "progress",
                    questTarget(wolfLevel), Math.max(0, questTarget(wolfLevel) - data.getWolfQuestCounter())), true);
        }
    }

    /** L7→8: taming wild adult wolves in wolf form (source GenericEvents wolf-interact, 1-in-3). */
    @SubscribeEvent
    public static void onTameQuestWolf(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Wolf wolf)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getWerewolfLevel() != 7 || data.getWolfQuestState() != WolfQuestState.STARTED) return;
        if (!WerewolfShiftUtil.isTransformed(data)) return;
        if (wolf.isTame() || wolf.isBaby()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        event.setCanceled(true);
        if (level.random.nextInt(3) == 0) {
            wolf.tame(player);
            data.incrementWolfQuestCounter();
            sync(player, data);
            player.displayClientMessage(Component.translatable("witchery.werewolf.level8progress",
                    6, Math.max(0, 6 - data.getWolfQuestCounter())), true);
        }
    }

    /** Per-level quest-counter targets for the combat-driven levels (matches WolfAltarBlock). */
    static int questTarget(int wolfLevel) {
        return switch (wolfLevel) {
            case 5 -> 10;
            case 6 -> 16;
            case 7 -> 6;
            case 8 -> 30;
            case 9 -> 1;
            default -> 0;
        };
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
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
        // Level-scaled wolf-form buffs. Simplified port of source Shapeshift.getStatBoost (the exact
        // per-level/per-form StatBoost attribute arrays land in Phase 3 with form selection); here the
        // amplifiers ramp with the werewolf level instead of being flat.
        int wolfLevel = Math.max(1, data.getWerewolfLevel());
        int speedAmp = Math.min(3, 1 + (wolfLevel - 1) / 3);  // Speed II → IV
        int strengthAmp = Math.min(2, (wolfLevel - 1) / 3);   // Strength I → III
        int jumpAmp = Math.min(2, (wolfLevel - 1) / 4);       // Jump Boost I → III
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,        EFFECT_DURATION, speedAmp, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,     EFFECT_DURATION, strengthAmp, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,   EFFECT_DURATION, jumpAmp, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER,       EFFECT_DURATION, 1, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, true, false));
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
