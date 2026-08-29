package com.emoniph.witchery.events;

import com.emoniph.witchery.blocks.entity.BloodCrucibleBlockEntity;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.AttackBatEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WereVillagerEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import com.emoniph.witchery.item.VampireClothingUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.util.VampireCage;
import com.emoniph.witchery.util.CreatureSunlight;
import com.emoniph.witchery.util.VillagerBlood;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles vampire gameplay: blood drain, blood power, sunburn, passive effects, biting, mesmerise,
 * and level-up quests.
 *
 * Human-blood thresholds (bloodLevel 0–500):
 *   0–50   → frenzy: Weakness II + Hunger II
 *   51–150 → hungry: Weakness I
 *   151–349→ normal: Night Vision
 *   350–500→ well-fed: Night Vision + Speed I + Strength I
 *
 * Blood power (bloodPower 0–maxBloodPower):
 *   max = 500 + level * 250
 *   Fills hunger bars (5 power → 1 bar); 0 power + 0 hunger → severe debuffs.
 *   Sunlight < level 5: drains all power. Level ≥ 5: drains 60 per 40 ticks.
 */
@EventBusSubscriber
public class VampireEvents {

    private static final int EFFECT_INTERVAL         = 100;
    private static final int EFFECT_DURATION         = 140;
    private static final int BLOOD_POWER_INTERVAL    = 40;   // matches 1.7.10 source
    private static final int SYNC_INTERVAL           = 200;
    private static final int MESMERISE_BLOOD_POWER_COST = 50;
    private static final int MESMERISE_MIN_LEVEL        = 2;
    private static final int VILLAGER_MAX_BLOOD      = VillagerBlood.MAX;
    private static final int VILLAGER_BLOOD_DRAIN    = 10;
    private static final int VILLAGER_LOW_BLOOD      = VillagerBlood.LOW;

    // Blood power gained per bite by entity type (source: GenericEvents.java)
    private static final int BP_GAIN_ANIMAL          = 2;

    // Quest thresholds
    private static final int QUEST_VILLAGER_BITES    = 5;
    private static final int QUEST_VILLAGER_LOW      = 250;
    private static final int QUEST_VILLAGER_HIGH     = 280;
    private static final int QUEST_VILLAGER_RESET    = 240;
    private static final int QUEST_SUN_TICKS         = 300;

    // Sunlight blood power drain per 40-tick interval at level ≥ 5
    private static final int SUNLIGHT_BP_DRAIN_HIGH  = 60;

    // Level 7→8: 3 distinct villages must be visited.
    private static final int QUEST_VILLAGES_REQUIRED = 3;
    // Coarse cell size (1<<6 = 64 blocks) used to treat one village as a single visit, matching the
    // ~64-block reach of the village POI test. The visited set is persisted in the player's vampire state.
    private static final int VILLAGE_CELL_SHIFT      = 6;
    private static final int SWARM_BAT_COUNT         = 15;
    private static final int VAMPIRE_POWER_COOLDOWN  = 10;
    private static final Identifier BAT_ATTACK_MODIFIER =
            Identifier.fromNamespaceAndPath("witchery", "vampire_bat_attack");
    private static final Identifier BAT_STEP_MODIFIER =
            Identifier.fromNamespaceAndPath("witchery", "vampire_bat_step_height");
    private static final Identifier VAMPIRE_ATTACK_MODIFIER =
            Identifier.fromNamespaceAndPath("witchery", "vampire_attack_damage");
    private static final ResourceKey<DamageType> VAMPIRE_FIRE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("witchery", "vampire_fire"));
    private static final ThreadLocal<Boolean> BLOOD_DRINK_DAMAGE = ThreadLocal.withInitial(() -> false);

    // ── Tick ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        data.tickVampirePowerCooldown();
        applyVampireState(player, data);
        int tick = player.tickCount;
        if (!data.isVampire()) {
            if (tick % BLOOD_POWER_INTERVAL == 0 && data.getHumanBlood() < VILLAGER_MAX_BLOOD) {
                data.giveHumanBlood(2);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        SyncPlayerDataPayload.from(player, data));
            }
            return;
        }

        if (tick % WitcheryConfig.VAMPIRE_BLOOD_DRAIN_INTERVAL.get() == 0) drainBlood(player, data);
        if (tick % WitcheryConfig.VAMPIRE_SUNBURN_CHECK_INTERVAL.get() == 0) checkSunburn(player, data);
        if (tick % EFFECT_INTERVAL == 0)      applyBloodEffects(player, data);
        if (tick % BLOOD_POWER_INTERVAL == 0) {
            tickVampirePhysiology(player);
            tickBloodPower(player, data);
        }
        if (tick % SYNC_INTERVAL == 0)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Bite ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer vampire)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        WitcheryPlayerData data = WitcheryAttachments.get(vampire);
        if (!data.isVampire()) return;
        switch (data.getSelectedVampirePower()) {
            case DRINK -> {
                event.setCanceled(true);
                double range = target.hasEffect(WitcheryMobEffects.PARALYSED) ? 2.1D : 1.3D;
                double dx = target.getX() - vampire.getX();
                double dz = target.getZ() - vampire.getZ();
                if (dx * dx + dz * dz > range * range) return;
                bite(vampire, target, data);
            }
            case MESMERIZE -> {
                event.setCanceled(true);
                if (vampire.isShiftKeyDown()) {
                    toggleVampireVision(vampire, data);
                } else if (!isHumanForm(data) || !canMesmerise(target, data)) {
                    playMesmeriseFailure(vampire);
                } else {
                    mesmerise(vampire, target, data);
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onVampireSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (WitcheryAttachments.get(player).isVampireBatForm()) {
            event.setNewSize(net.minecraft.world.entity.EntityDimensions.scalable(0.3F, 0.6F).withEyeHeight(0.48F));
        }
    }

    // Garlic counterplay

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) return;

        BlockState state = player.level().getBlockState(event.getPos());
        if (state.is(WitcheryBlocks.GARLIC_GARLAND.get())) {
            player.igniteForSeconds(1.0f);
            event.setCanceled(true);
            return;
        }

        boolean toolHarvestsStone = player.getMainHandItem().isCorrectToolForDrops(
                Blocks.STONE.defaultBlockState());
        if (!sourceCanCrushBlock(data.getVampireLevel(), isHumanForm(data), player.isShiftKeyDown(),
                toolHarvestsStone, player.getFoodData().getFoodLevel(), state)) return;

        if (tryVampireCrush(player, event.getPos(), state)) {
            player.causeFoodExhaustion(10.0F);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVampireCombatDamage(LivingDamageEvent.Pre event) {
        if (BLOOD_DRINK_DAMAGE.get() || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(attacker);
        if (!sourceCanLaunch(data.getVampireLevel(), isHumanForm(data), attacker.isShiftKeyDown())) return;

        event.getEntity().setDeltaMovement(sourceLaunchMotion(attacker.getLookAngle()));
        event.getEntity().hurtMarked = true;
    }

    // ── Blood power tick ─────────────────────────────────────────────────────

    private static void tickBloodPower(ServerPlayer player, WitcheryPlayerData data) {
        if (data.isVampireBatForm()
                && !spendBloodPower(player, data, WitcheryPlayerData.VampirePower.BAT.upkeepCost(), true)) {
            setBatForm(player, data, false);
        }
        if (data.isVampireVisionActive()) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
        }
        // Sunlight blood power drain — the "Resist Sun" mechanic (source GenericEvents onPlayerTick).
        if (isInSunlight(player) && !player.isCreative()) {
            // A starved vampire caught in daylight past the ~20s grace period dies outright.
            if (data.getBloodPower() == 0 && player.tickCount > 400) {
                player.hurt(player.damageSources().onFire(), Float.MAX_VALUE);
            }
            if (data.getVampireLevel() >= 5) {
                // Resist Sun: blood power buys survival in daylight, bleeding off 60/interval with a
                // matching debuff load (source Weakness IV + Slowness I + Mining Fatigue I) instead of
                // instantly losing all power like a fledgling.
                data.spendBloodPower(SUNLIGHT_BP_DRAIN_HIGH, false);
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,        200, 3, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,        200, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,  200, 0, true, false));
            } else {
                data.setBloodPower(0);
            }
            if (data.getBloodPower() == 0) {
                player.igniteForSeconds(5.0f);
            }
        }

        // Fill hunger bars from blood power (5 power → 1 food bar + 4 saturation)
        var food = player.getFoodData();
        boolean spent = false;
        while (food.getFoodLevel() < 20 && data.spendBloodPower(5)) {
            food.eat(1, 4.0f);
            spent = true;
        }

        // No blood power + no food → severe debuffs (Weakness IX + Poison II + Nausea II)
        if (data.getBloodPower() == 0 && food.getFoodLevel() == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 8, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.POISON,   200, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 1, true, false));
        }

        // Level 1→2: auto-level when blood power fills
        if (data.getVampireLevel() == 1 && data.getBloodPower() >= data.getMaxBloodPower()) {
            levelUp(player, data);
            return; // levelUp syncs
        }

        // Level 3→4: sun-endurance quest (must be in sunlight, checked every 40 ticks)
        if (data.getVampireLevel() == 3 && player.level().getDayTime() % 24000 < 12000) {
            if (data.getVampireQuestCounter() >= QUEST_SUN_TICKS) {
                if (data.canIncreaseVampireLevel()) levelUp(player, data);
            } else {
                data.incrementQuestCounter();
            }
        }

        // Auto-level for 4, 5, 9 when blood power reaches max
        int vl = data.getVampireLevel();
        if ((vl == 4 || vl == 5 || vl == 9)
                && data.getBloodPower() >= data.getMaxBloodPower()
                && data.canIncreaseVampireLevel()) {
            levelUp(player, data);
            return;
        }

        // Level 7→8: visit 3 distinct villages (source GenericEvents village-chunk quest).
        if (data.getVampireLevel() == 7 && data.canIncreaseVampireLevel()) {
            checkVillageVisitQuest(player, data);
        }

        if (spent) {
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        }
    }

    // ── Sunburn ───────────────────────────────────────────────────────────────

    /** Resist Sun: a level-5+ vampire with blood power to burn endures daylight (with heavy debuffs). */
    static boolean resistsSun(WitcheryPlayerData data) {
        return data.getVampireLevel() >= 5 && data.getBloodPower() > 0;
    }

    private static void checkSunburn(ServerPlayer player, WitcheryPlayerData data) {
        if (!isInSunlight(player)) return;
        if (resistsSun(data)) return; // the Resist Sun ability spares the flat sunburn tick
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return; // helmet blocks sunburn

        // Fire protection from full vampire clothing sets
        if (VampireClothingUtil.isBasicFlameProtectionActive(player)) return;

        player.hurt(player.damageSources().inFire(), WitcheryConfig.VAMPIRE_SUNBURN_DAMAGE.get());
        if (player.tickCount % 100 == 0) {
            player.displayClientMessage(Component.translatable("witchery.vampire.sunburn"), false);
        }
    }

    // ── Passive effects ───────────────────────────────────────────────────────

    private static void drainBlood(ServerPlayer player, WitcheryPlayerData data) {
        data.addBlood(-1);
        if (data.getBloodLevel() == 50) {
            player.displayClientMessage(Component.translatable("witchery.vampire.frenzy"), false);
        }
    }

    /** Human-blood tier (bloodLevel 0–500), per the source thresholds documented at the top of this file. */
    enum BloodTier { FRENZY, HUNGRY, NEUTRAL, WELL_FED }

    static BloodTier bloodTier(int blood) {
        if (blood <= 50) return BloodTier.FRENZY;
        if (blood <= 150) return BloodTier.HUNGRY;
        if (blood >= 350) return BloodTier.WELL_FED;
        return BloodTier.NEUTRAL;
    }

    private static void applyBloodEffects(ServerPlayer player, WitcheryPlayerData data) {
        switch (bloodTier(data.getBloodLevel())) {
            case FRENZY -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 1, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER,   EFFECT_DURATION, 1, true, true));
            }
            case HUNGRY -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, true, false));
            case WELL_FED -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED,        EFFECT_DURATION, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,     EFFECT_DURATION, 0, true, false));
            }
            case NEUTRAL -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, true, false));
        }
    }

    // ── Bite ─────────────────────────────────────────────────────────────────

    private static void bite(ServerPlayer vampire, LivingEntity target, WitcheryPlayerData data) {
        if (isWerewolfTarget(target)) {
            hurtFromBloodDrinking(vampire, vampire, 4.0F);
            if (vampire.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.8D, target.getZ(),
                        16, 0.5D, 0.2D, 0.5D, 0.02D);
            }
            vampire.level().playSound(null, target.blockPosition(), WitcherySoundEvents.RANDOM_DRINK.get(),
                    SoundSource.PLAYERS, 0.5F, 1.0F);
            return;
        }
        int gained = drainBloodFromTarget(vampire, target);
        if (gained <= 0) {
            vampire.displayClientMessage(Component.translatable("witchery.vampire.bite.dry",
                    target.getName()), true);
            playMesmeriseFailure(vampire);
            return;
        }

        data.addBlood(gained);

        // Blood power gain from bite (1.7.10 GenericEvents source)
        int bpGain = bloodPowerGain(vampire, target, gained);
        if (bpGain > 0) data.addBloodPower(bpGain);

        // Level 2→3 quest: bite villager 5× when villager blood hits 250-280 range
        if (target instanceof Villager villager && data.getVampireLevel() == 2) {
            int remaining = VillagerBlood.get(villager);
            checkVillagerBloodQuest(vampire, data, remaining);
        }

        // Level 8→9 quest: bite a caged villager 5× while its blood sits in the 250-280 window.
        if (target instanceof Villager caged && data.getVampireLevel() == 8
                && isVillagerCaged(vampire.level(), caged.blockPosition())) {
            int remaining = VillagerBlood.get(caged);
            checkVillagerBloodQuest(vampire, data, remaining);
        }

        tryFillNearbyBloodCrucible(vampire);
        alertBloodDrinkingWitnesses(vampire, target);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));
        if (target instanceof ServerPlayer playerTarget) {
            WitcheryPlayerData targetData = WitcheryAttachments.get(playerTarget);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(playerTarget,
                    SyncPlayerDataPayload.from(playerTarget, targetData));
        }

        playBloodDrinkFeedback(vampire, target);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.bite.success",
                target.getName(), gained, data.getBloodLevel()), false);
    }

    /** Source vampires breathe indefinitely, shrug off poison, and cannot hide from flame behind Fire Resistance. */
    static void tickVampirePhysiology(ServerPlayer player) {
        if (player.isInWater()) {
            player.setAirSupply(300);
        }
        player.removeEffect(MobEffects.POISON);
        if (player.isOnFire() && player.hasEffect(MobEffects.FIRE_RESISTANCE)
                && player.level() instanceof ServerLevel level) {
            player.hurtServer(level, vampireFireDamageSource(level), 2.0F);
        }
    }

    static DamageSource vampireFireDamageSource(ServerLevel level) {
        return level.damageSources().source(VAMPIRE_FIRE_DAMAGE);
    }

    private static void playBloodDrinkFeedback(ServerPlayer vampire, LivingEntity target) {
        if (!(vampire.level() instanceof ServerLevel level)) {
            return;
        }
        double y = target.getY() + target.getBbHeight() * 0.8D;
        level.playSound(null, target.getX(), y, target.getZ(), WitcherySoundEvents.RANDOM_DRINK.get(),
                SoundSource.PLAYERS, 0.5F, 1.0F);
        level.sendParticles(DustParticleOptions.REDSTONE,
                target.getX(), y, target.getZ(),
                10, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.1D,
                target.getBbWidth() * 0.35D, 0.0D);
    }

    private static int drainBloodFromTarget(ServerPlayer vampire, LivingEntity target) {
        if (target instanceof ServerPlayer playerTarget) {
            WitcheryPlayerData targetData = WitcheryAttachments.get(playerTarget);
            int taken = targetData.takeHumanBlood(drinkAmount(vampire), playerTarget.isSleeping());
            if (targetData.getHumanBlood() < VILLAGER_LOW_BLOOD) {
                hurtFromBloodDrinking(vampire, playerTarget, 1.0F);
            } else if (!playerTarget.isSleeping()) {
                hurtFromBloodDrinking(vampire, playerTarget, 0.1F);
            }
            return taken;
        }
        if (target instanceof Villager villager) {
            return drainVillagerBlood(vampire, villager);
        }
        if (target instanceof VillageGuardEntity guard) {
            return drainGuardBlood(vampire, guard);
        }
        if (target instanceof Mob) {
            int gained = bloodValue(vampire, target);
            hurtFromBloodDrinking(vampire, target, 1.0F);
            return gained;
        }
        return 0;
    }

    private static int drainGuardBlood(ServerPlayer vampire, VillageGuardEntity guard) {
        BLOOD_DRINK_DAMAGE.set(true);
        try {
            return guard.takeBlood(drinkAmount(vampire), vampire);
        } finally {
            BLOOD_DRINK_DAMAGE.remove();
        }
    }

    private static int drainVillagerBlood(ServerPlayer vampire, Villager villager) {
        boolean knockedOut = VillagerBlood.isKnockedOut(villager);
        int taken = VillagerBlood.take(villager, drinkAmount(vampire));
        int remaining = VillagerBlood.get(villager);

        if (remaining < VILLAGER_LOW_BLOOD) {
            hurtFromBloodDrinking(vampire, villager, 1.3F);
        } else if (!knockedOut) {
            hurtFromBloodDrinking(vampire, villager, 0.1F);
        }
        return taken;
    }

    private static void hurtFromBloodDrinking(ServerPlayer vampire, LivingEntity target, float amount) {
        BLOOD_DRINK_DAMAGE.set(true);
        try {
            target.hurt(vampire.damageSources().playerAttack(vampire), amount);
        } finally {
            BLOOD_DRINK_DAMAGE.remove();
        }
    }

    private static int drinkAmount(ServerPlayer vampire) {
        if (WitcheryAttachments.get(vampire).isVampireBatForm()) return 2;
        return VampireClothingUtil.isDrinkBoostActive(vampire) ? 15 : VILLAGER_BLOOD_DRAIN;
    }

    /** Blood power gained per bite. Source: GenericEvents.java (EntityInteract handler). */
    private static int bloodPowerGain(ServerPlayer vampire, LivingEntity target, int bloodTaken) {
        if (target instanceof Villager || target instanceof VillageGuardEntity || target instanceof Player) {
            return bloodTaken;
        }
        if (target instanceof Mob) {
            WitcheryPlayerData data = WitcheryAttachments.get(vampire);
            return sourceCreatureBloodPowerGain(data.getBloodPower(), data.getMaxBloodPower());
        }
        return 0;
    }

    private static boolean isWerewolfTarget(LivingEntity target) {
        if (target instanceof WereVillagerEntity || target instanceof WolfmanEntity) return true;
        return target instanceof ServerPlayer player && WitcheryAttachments.get(player).isWerewolf();
    }

    /** Level 2→3 quest: biting villager at precise blood range. */
    private static void checkVillagerBloodQuest(ServerPlayer vampire, WitcheryPlayerData data, int remaining) {
        if (remaining >= QUEST_VILLAGER_LOW && remaining <= QUEST_VILLAGER_HIGH) {
            if (data.getVampireQuestCounter() >= QUEST_VILLAGER_BITES) {
                levelUp(vampire, data);
            } else {
                data.incrementQuestCounter();
                vampire.level().playSound(null, vampire.blockPosition(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                vampire.displayClientMessage(Component.translatable("witchery.vampire.quest.progress",
                        data.getVampireQuestCounter(), QUEST_VILLAGER_BITES), true);
            }
        } else if (remaining < QUEST_VILLAGER_RESET) {
            data.resetQuestCounter();
        }
    }

    /** Level 7→8 quest: standing in three distinct villages (persisted in the player's vampire state). */
    private static void checkVillageVisitQuest(ServerPlayer player, WitcheryPlayerData data) {
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = player.blockPosition();
        if (!level.isVillage(pos)) return;

        long cell = (((long) (pos.getX() >> VILLAGE_CELL_SHIFT)) << 32)
                ^ ((pos.getZ() >> VILLAGE_CELL_SHIFT) & 0xFFFFFFFFL);
        if (!data.addVisitedVillage(cell)) return; // already counted this village

        if (data.getVisitedVillageCount() >= QUEST_VILLAGES_REQUIRED) {
            data.clearVisitedVillages();
            levelUp(player, data);
        } else {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("witchery.vampire.quest.village_progress",
                    data.getVisitedVillageCount(), QUEST_VILLAGES_REQUIRED), true);
        }
    }

    /** Source {@code GenericEvents.villagerIsInCage}: an iron-bar holding cell around the villager. */
    private static boolean isVillagerCaged(Level level, BlockPos pos) {
        return VampireCage.isCagedAround(pos.getX(), pos.getY(), pos.getZ(), new VampireCage.BlockQuery() {
            @Override public boolean isIronBars(int x, int y, int z) {
                return level.getBlockState(new BlockPos(x, y, z)).is(Blocks.IRON_BARS);
            }
            @Override public boolean isSolidRoof(int x, int y, int z) {
                return !level.getBlockState(new BlockPos(x, y, z)).canBeReplaced();
            }
        });
    }

    // ── Level-up ──────────────────────────────────────────────────────────────

    private static void levelUp(ServerPlayer player, WitcheryPlayerData data) {
        data.setVampireLevel(data.getVampireLevel() + 1);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(
                Component.translatable("witchery.vampire.level_up", data.getVampireLevel()), false);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Mesmerise ─────────────────────────────────────────────────────────────

    private static void mesmerise(ServerPlayer vampire, LivingEntity target, WitcheryPlayerData data) {
        if (target.hasEffect(WitcheryMobEffects.PARALYSED)) {
            playMesmeriseFailure(vampire);
            vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.already",
                    target.getName()), true);
            return;
        }
        if (!vampire.isCreative() && data.getBloodPower() < MESMERISE_BLOOD_POWER_COST) {
            playMesmeriseFailure(vampire);
            vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.not_enough_blood",
                    MESMERISE_BLOOD_POWER_COST), true);
            return;
        }

        int duration = sourceMesmeriseDuration(data.getVampireLevel(),
                VampireClothingUtil.isMesmeriseBoostActive(vampire));
        int amplifier = sourceMesmeriseAmplifier(data.getVampireLevel());
        spendBloodPower(vampire, data, MESMERISE_BLOOD_POWER_COST, true);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));
        target.addEffect(new MobEffectInstance(WitcheryMobEffects.PARALYSED, duration, amplifier, true, true));
        vampire.level().playSound(null, target.blockPosition(),
                WitcherySoundEvents.RANDOM_HYPNOSIS.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.success",
                target.getName(), duration / 20), true);
    }

    public static void handleVampirePowerRequest(ServerPlayer vampire,
                                                  WitcheryPlayerData.VampirePower requested,
                                                  boolean trigger) {
        WitcheryPlayerData data = WitcheryAttachments.get(vampire);
        data.setSelectedVampirePower(requested);
        if (trigger && data.getSelectedVampirePower() == requested) {
            tryActivateSelectedPower(vampire, data);
        }
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));
    }

    public static boolean tryActivateSelectedPower(ServerPlayer vampire, WitcheryPlayerData data) {
        if (!data.isVampire()) return false;
        if (data.getVampirePowerCooldown() > 0) {
            playMesmeriseFailure(vampire);
            return false;
        }
        data.setVampirePowerCooldown(VAMPIRE_POWER_COOLDOWN);
        return switch (data.getSelectedVampirePower()) {
            case MESMERIZE -> activateVisionPower(vampire, data);
            case SPEED -> activateSpeedPower(vampire, data);
            case BAT -> activateBatPower(vampire, data);
            case ULTIMATE -> activateUltimatePower(vampire, data);
            default -> false;
        };
    }

    private static boolean activateVisionPower(ServerPlayer vampire, WitcheryPlayerData data) {
        if (!vampire.isShiftKeyDown()) return true;
        toggleVampireVision(vampire, data);
        return true;
    }

    private static boolean activateSpeedPower(ServerPlayer vampire, WitcheryPlayerData data) {
        MobEffectInstance speed = vampire.getEffect(MobEffects.SPEED);
        int currentStage = sourceSpeedStage(speed == null ? -1 : speed.getAmplifier());
        if (!isHumanForm(data)
                || data.getVampireLevel() < WitcheryPlayerData.VampirePower.SPEED.unlockLevel()
                || currentStage > sourceMaxSpeedStage(data.getVampireLevel())
                || !spendBloodPower(vampire, data, WitcheryPlayerData.VampirePower.SPEED.initialCost(), true)) {
            playMesmeriseFailure(vampire);
            return false;
        }

        int amplifier = speed == null ? 2 : (speed.getAmplifier() + 1) * 2 - 1;
        int duration = speed == null ? 10 * 20 : speed.getDuration() + 60;
        vampire.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, amplifier, true, false));
        vampire.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, currentStage + 1, true, false));
        playPowerSuccess(vampire);
        return true;
    }

    private static boolean activateBatPower(ServerPlayer vampire, WitcheryPlayerData data) {
        if (data.getVampireLevel() < WitcheryPlayerData.VampirePower.BAT.unlockLevel()) {
            playMesmeriseFailure(vampire);
            return false;
        }
        if (data.isVampireBatForm()) {
            setBatForm(vampire, data, false);
            playPowerSuccess(vampire);
            return true;
        }
        if (!isHumanForm(data)
                || !spendBloodPower(vampire, data, WitcheryPlayerData.VampirePower.BAT.initialCost(), true)) {
            playMesmeriseFailure(vampire);
            return false;
        }
        setBatForm(vampire, data, true);
        playPowerSuccess(vampire);
        return true;
    }

    private static boolean activateUltimatePower(ServerPlayer vampire, WitcheryPlayerData data) {
        if (data.getVampireLevel() < WitcheryPlayerData.VampirePower.ULTIMATE.unlockLevel()
                || data.getVampireUltimateCharges() <= 0
                || !isHumanForm(data)
                || !(vampire.level() instanceof ServerLevel level)) {
            playUltimateFailure(vampire, data);
            return false;
        }

        boolean success = switch (data.getVampireUltimate()) {
            case STORM -> activateStormUltimate(vampire, level);
            case SWARM -> activateSwarmUltimate(vampire, level);
            case FARM -> activateFarmUltimate(vampire);
            case NONE -> false;
        };
        if (!success) {
            playMesmeriseFailure(vampire);
            return false;
        }
        if (!vampire.isCreative()) {
            data.spendVampireUltimateCharge();
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(vampire, SyncPlayerDataPayload.from(vampire, data));
        return true;
    }

    private static boolean activateStormUltimate(ServerPlayer vampire, ServerLevel level) {
        if (level.isRaining()) return false;
        int duration = (300 + level.random.nextInt(600)) * 20;
        level.setWeatherParameters(0, duration, true, true);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.ultimate.storm"), true);
        playPowerSuccess(vampire);
        return true;
    }

    private static boolean activateSwarmUltimate(ServerPlayer vampire, ServerLevel level) {

        for (int i = 0; i < SWARM_BAT_COUNT; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.75D + level.random.nextDouble() * 2.25D;
            double x = vampire.getX() + Math.cos(angle) * radius;
            double y = vampire.getY() + 2.5D + level.random.nextDouble() * 1.5D;
            double z = vampire.getZ() + Math.sin(angle) * radius;
            AttackBatEntity bat = AttackBatEntity.create(level, vampire, x, y, z);
            if (bat != null) {
                level.addFreshEntity(bat);
            }
        }

        level.playSound(null, vampire.blockPosition(), SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 1.4F, 0.65F);
        level.playSound(null, vampire.blockPosition(), SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 0.8F, 0.8F);
        level.sendParticles(ParticleTypes.SMOKE, vampire.getX(), vampire.getY() + 1.5D, vampire.getZ(),
                32, 1.5D, 1.2D, 1.5D, 0.04D);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.ultimate.swarm"), true);
        return true;
    }

    private static boolean activateFarmUltimate(ServerPlayer vampire) {
        if (vampire.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return false;
        var server = vampire.level().getServer();
        if (server == null) return false;

        ServerLevel destination = (ServerLevel) vampire.level();
        BlockPos target = null;
        ServerPlayer.RespawnConfig respawnConfig = vampire.getRespawnConfig();
        if (respawnConfig != null && respawnConfig.respawnData() != null) {
            var respawn = respawnConfig.respawnData();
            ServerLevel respawnLevel = server.getLevel(respawn.dimension());
            if (respawnLevel != null) {
                destination = respawnLevel;
                target = respawn.pos();
            }
        }
        if (target == null) {
            destination = server.overworld();
            target = destination.getLevelData().getRespawnData().pos();
        }

        if (destination == vampire.level() && vampire.blockPosition().distSqr(target) <= 36.0D) {
            BlockPos origin = vampire.blockPosition();
            target = destination.getPoiManager().findAll(
                            holder -> holder.is(PoiTypes.HOME), pos -> origin.distSqr(pos) > 36.0D,
                            origin, 512, PoiManager.Occupancy.ANY)
                    .min(java.util.Comparator.comparingDouble(origin::distSqr))
                    .orElse(null);
            if (target == null) return false;
        }

        BlockPos safe = com.emoniph.witchery.world.MirrorHelper.findSafeLanding(destination, target, 8);
        vampire.teleportTo(destination, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                java.util.Set.of(), vampire.getYRot(), vampire.getXRot(), false);
        vampire.setDeltaMovement(0.0D, 0.0D, 0.0D);
        vampire.fallDistance = 0.0F;
        destination.playSound(null, safe, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.8F);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.ultimate.farm"), true);
        return true;
    }

    private static boolean isVampireTransfixed(LivingEntity target) {
        MobEffectInstance paralysis = target.getEffect(WitcheryMobEffects.PARALYSED);
        return sourceVampireTransfixAmplifier(paralysis == null ? -1 : paralysis.getAmplifier());
    }

    private static boolean canMesmerise(LivingEntity target, WitcheryPlayerData data) {
        if (data.getVampireLevel() < MESMERISE_MIN_LEVEL) return false;
        return sourceMesmeriseTarget(
                target instanceof Villager && !(target instanceof WereVillagerEntity),
                target instanceof Player,
                target instanceof VillageGuardEntity);
    }

    private static void playMesmeriseFailure(ServerPlayer vampire) {
        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 0.5F);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void playPowerSuccess(ServerPlayer vampire) {
        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.55F, 1.4F);
    }

    private static void playUltimateFailure(ServerPlayer vampire, WitcheryPlayerData data) {
        if (data.getVampireUltimateCharges() <= 0) {
            vampire.displayClientMessage(Component.translatable("witchery.vampire.ultimate.no_charges"), true);
        }
        playMesmeriseFailure(vampire);
    }

    private static void toggleVampireVision(ServerPlayer vampire, WitcheryPlayerData data) {
        boolean active = !data.isVampireVisionActive();
        data.setVampireVisionActive(active);
        if (active) {
            vampire.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
        } else {
            vampire.removeEffect(MobEffects.NIGHT_VISION);
        }
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));
    }

    private static boolean spendBloodPower(ServerPlayer vampire, WitcheryPlayerData data,
                                           int amount, boolean exact) {
        return vampire.isCreative() || data.spendBloodPower(amount, exact);
    }

    private static boolean isHumanForm(WitcheryPlayerData data) {
        return !data.isVampireBatForm() && data.getTransformationLevel() <= 0.01F;
    }

    private static void setBatForm(ServerPlayer vampire, WitcheryPlayerData data, boolean active) {
        data.setVampireBatForm(active);
        applyVampireState(vampire, data);
        vampire.refreshDimensions();
        if (vampire.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE,
                    vampire.getX(), vampire.getY() + 0.6D, vampire.getZ(),
                    18, 0.35D, 0.35D, 0.35D, 0.02D);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(vampire, SyncPlayerDataPayload.from(vampire, data));
    }

    static void applyVampireState(ServerPlayer vampire, WitcheryPlayerData data) {
        boolean bat = data.isVampireBatForm();
        AttributeInstance attack = vampire.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            if (bat) {
                attack.removeModifier(VAMPIRE_ATTACK_MODIFIER);
                attack.addOrUpdateTransientModifier(new AttributeModifier(BAT_ATTACK_MODIFIER,
                        -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                attack.removeModifier(BAT_ATTACK_MODIFIER);
                double damageBoost = data.isVampire() && isHumanForm(data)
                        ? sourceVampireDamageBoost(data.getVampireLevel()) : 0.0D;
                if (damageBoost != 0.0D) {
                    attack.addOrUpdateTransientModifier(new AttributeModifier(VAMPIRE_ATTACK_MODIFIER,
                            damageBoost, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                } else {
                    attack.removeModifier(VAMPIRE_ATTACK_MODIFIER);
                }
            }
        }
        AttributeInstance stepHeight = vampire.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            if (bat) {
                stepHeight.addOrUpdateTransientModifier(new AttributeModifier(BAT_STEP_MODIFIER,
                        -0.1D, AttributeModifier.Operation.ADD_VALUE));
            } else {
                stepHeight.removeModifier(BAT_STEP_MODIFIER);
            }
        }

        if (bat) vampire.fallDistance = 0.0F;
        var abilities = vampire.getAbilities();
        boolean oldMayfly = abilities.mayfly;
        boolean oldFlying = abilities.flying;
        VampireFlightOwnership.Decision flight = VampireFlightOwnership.reconcile(
                bat, vampire.isCreative() || vampire.isSpectator(),
                abilities.mayfly, abilities.flying,
                data.witcheryOwnsBatFlight(), data.mayflyBeforeBat(), data.flyingBeforeBat());
        abilities.mayfly = flight.mayfly();
        abilities.flying = flight.flying();
        data.setBatFlightOwnership(flight.ownsGrant(), flight.mayflyBefore(), flight.flyingBefore());
        if (oldMayfly != abilities.mayfly || oldFlying != abilities.flying) {
            vampire.onUpdateAbilities();
        }
    }

    static boolean tryVampireCrush(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        ItemStack tool = player.getMainHandItem();
        boolean needsSourceDrops = !player.hasCorrectToolForDrops(state);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!player.gameMode.destroyBlock(pos)) return false;
        if (needsSourceDrops) {
            Block.dropResources(state, level, pos, blockEntity, player, tool);
        }
        return true;
    }

    static double sourceVampireDamageBoost(int vampireLevel) {
        int level = Math.clamp(vampireLevel, 0, 10);
        if (level >= 7) return 3.0D;
        if (level >= 4) return 2.0D;
        return level >= 1 ? 1.0D : 0.0D;
    }

    static boolean sourceCanLaunch(int vampireLevel, boolean humanForm, boolean sneaking) {
        return vampireLevel >= 3 && humanForm && sneaking;
    }

    static Vec3 sourceLaunchMotion(Vec3 look) {
        return new Vec3(look.x * 1.8D, 0.9D, look.z * 1.8D);
    }

    static boolean sourceCanCrushBlock(int vampireLevel, boolean humanForm, boolean sneaking,
                                       boolean toolHarvestsStone, int foodLevel, BlockState state) {
        return vampireLevel >= 6 && humanForm && sneaking && !toolHarvestsStone && foodLevel > 0
                && (state.is(Blocks.STONE) || state.is(Blocks.NETHERRACK) || state.is(Blocks.COBBLESTONE));
    }

    static void alertBloodDrinkingWitnesses(ServerPlayer vampire, LivingEntity victim) {
        if (!(victim instanceof Villager) && !(victim instanceof VillageGuardEntity)) return;
        AABB bounds = victim.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        for (VillageGuardEntity guard : victim.level().getEntitiesOfClass(VillageGuardEntity.class, bounds)) {
            if (!guard.hasEffect(WitcheryMobEffects.PARALYSED)
                    && guard.getSensing().hasLineOfSight(victim)) {
                guard.setTarget(vampire);
            }
        }
    }

    static int sourceMesmeriseDuration(int vampireLevel, boolean clothingBoost) {
        int level = Math.clamp(vampireLevel, 0, 10);
        return (5 + level / 2 + Math.max(0, (level - 4) / 2) + (clothingBoost ? 3 : 0)) * 20;
    }

    static int sourceMesmeriseAmplifier(int vampireLevel) {
        return vampireLevel >= 8 ? 5 : 4;
    }

    static boolean sourceMesmeriseTarget(boolean ordinaryVillager, boolean player, boolean villageGuard) {
        return ordinaryVillager || player || villageGuard;
    }

    static int sourceCreatureBloodPowerGain(int currentBloodPower, int maxBloodPower) {
        int cap = (int) Math.ceil(Math.max(0, maxBloodPower) * 0.25F);
        return Math.max(0, Math.min(BP_GAIN_ANIMAL, cap - Math.max(0, currentBloodPower)));
    }

    static boolean sourceVampireTransfixAmplifier(int amplifier) {
        return amplifier >= 4;
    }

    static int sourceSpeedStage(int speedAmplifier) {
        if (speedAmplifier < 0) return 0;
        return (int) Math.ceil(Math.log(speedAmplifier + 1.0D) / Math.log(2.0D));
    }

    static int sourceMaxSpeedStage(int vampireLevel) {
        return (int) Math.ceil((Math.clamp(vampireLevel, 0, 10) - 3) / 2.0D);
    }

    private static boolean isInSunlight(ServerPlayer player) {
        // Source CreatureUtil.isInSunlight gates on the dimension actually having a sun (func_76569_d);
        // skyless dimensions like the Nether/End have no sunlight even where the heightmap is open.
        if (!CreatureSunlight.dimensionAllowsSunlight(player.level())) return false;
        if (player.level().getDayTime() % 24000 >= 12000) return false;
        BlockPos head = player.blockPosition().above();
        if (!player.level().canSeeSky(head)) return false;
        // Source CreatureUtil.isInSunlight spares vampires during rain in precipitating biomes;
        // isRainingAt is the vanilla equivalent (true only under open sky in a RAIN-precip biome, so
        // snowy biomes still burn, matching source's `isRaining() && biome.canRain()`).
        if (player.level().isRainingAt(head)) return false;
        return !player.isUnderWater() && !player.isInWater();
    }

    private static void tryFillNearbyBloodCrucible(ServerPlayer vampire) {
        Level level = vampire.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return;
        BlockPos origin = vampire.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-6, -2, -6), origin.offset(6, 2, 6))) {
            if (level.getBlockEntity(pos) instanceof BloodCrucibleBlockEntity crucible
                    && !crucible.isFull()) {
                crucible.increaseBloodLevel(level);
                break;
            }
        }
    }

    /** Human blood gained from biting different entity types. */
    private static int bloodValue(ServerPlayer vampire, LivingEntity entity) {
        int base;
        if (entity instanceof Monster) {
            base = 5;
        } else if (entity instanceof Animal) {
            base = 25;
        } else {
            base = 15;
        }
        if (!VampireClothingUtil.isDrinkBoostActive(vampire)) {
            return base;
        }
        return (base * 3 + 1) / 2;
    }
}
