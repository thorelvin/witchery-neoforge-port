package com.emoniph.witchery.events;

import com.emoniph.witchery.blocks.entity.BloodCrucibleBlockEntity;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.emoniph.witchery.item.VampireClothingUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.util.VampireCage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

    private static final int DRAIN_INTERVAL          = 100;  // humanBlood drain: 1/5 s
    private static final int EFFECT_INTERVAL         = 100;
    private static final int EFFECT_DURATION         = 140;
    private static final int BLOOD_POWER_INTERVAL    = 40;   // matches 1.7.10 source
    private static final int SYNC_INTERVAL           = 200;
    private static final int MESMERISE_BLOOD_POWER_COST = 50;
    private static final int MESMERISE_BASE_DURATION    = 5 * 20;
    private static final int MESMERISE_BOOST_DURATION   = 3 * 20;
    private static final int MESMERISE_SLOWNESS_AMPLIFIER = 4;
    private static final int MESMERISE_MIN_LEVEL        = 2;
    private static final int VILLAGER_MAX_BLOOD      = 500;
    private static final int VILLAGER_BLOOD_DRAIN    = 50;
    private static final int VILLAGER_LOW_BLOOD      = 250;
    private static final String VILLAGER_BLOOD_TAG   = "witchery_villager_blood";

    // Blood power gained per bite by entity type (source: GenericEvents.java)
    private static final int BP_GAIN_VILLAGER_BASE   = 10;
    private static final int BP_GAIN_VILLAGER_BOOST  = 15;  // with clothing drink boost
    private static final int BP_GAIN_ANIMAL          = 2;
    private static final int BP_GAIN_OTHER           = 3;

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

    // ── Tick ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) return;

        int tick = player.tickCount;
        if (tick % WitcheryConfig.VAMPIRE_BLOOD_DRAIN_INTERVAL.get() == 0) drainBlood(player, data);
        if (tick % WitcheryConfig.VAMPIRE_SUNBURN_CHECK_INTERVAL.get() == 0) checkSunburn(player, data);
        if (tick % EFFECT_INTERVAL == 0)      applyBloodEffects(player, data);
        if (tick % BLOOD_POWER_INTERVAL == 0) tickBloodPower(player, data);
        if (tick % SYNC_INTERVAL == 0)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Bite ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer vampire)) return;
        if (!vampire.getMainHandItem().isEmpty()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        WitcheryPlayerData data = WitcheryAttachments.get(vampire);
        if (!data.isVampire()) return;

        if (vampire.isShiftKeyDown() && canMesmerise(target, data)) {
            event.setCanceled(true);
            mesmerise(vampire, target, data);
            return;
        }
        if (target instanceof Player) return; // player biting deferred to M22+

        event.setCanceled(true);
        bite(vampire, target, data);
    }

    // Garlic counterplay

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;
        if (!player.level().getBlockState(event.getPos()).is(WitcheryBlocks.GARLIC_GARLAND.get())) return;

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) return;

        player.igniteForSeconds(1.0f);
        event.setCanceled(true);
    }

    // ── Blood power tick ─────────────────────────────────────────────────────

    private static void tickBloodPower(ServerPlayer player, WitcheryPlayerData data) {
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
                data.spendBloodPower(SUNLIGHT_BP_DRAIN_HIGH);
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
            int remaining = villager.getPersistentData().getIntOr(VILLAGER_BLOOD_TAG, VILLAGER_MAX_BLOOD);
            checkVillagerBloodQuest(vampire, data, remaining);
        }

        // Level 8→9 quest: bite a caged villager 5× while its blood sits in the 250-280 window.
        if (target instanceof Villager caged && data.getVampireLevel() == 8
                && isVillagerCaged(vampire.level(), caged.blockPosition())) {
            int remaining = caged.getPersistentData().getIntOr(VILLAGER_BLOOD_TAG, VILLAGER_MAX_BLOOD);
            checkVillagerBloodQuest(vampire, data, remaining);
        }

        tryFillNearbyBloodCrucible(vampire);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));

        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6f, 0.5f);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.bite.success",
                target.getName(), gained, data.getBloodLevel()), false);
    }

    private static int drainBloodFromTarget(ServerPlayer vampire, LivingEntity target) {
        if (target instanceof Villager villager) {
            return drainVillagerBlood(vampire, villager);
        }
        if (target instanceof VillageGuardEntity guard) {
            return drainGuardBlood(vampire, guard);
        }
        int gained = bloodValue(vampire, target);
        target.hurt(vampire.damageSources().playerAttack(vampire), 2f);
        return gained;
    }

    private static int drainGuardBlood(ServerPlayer vampire, VillageGuardEntity guard) {
        int desired = VILLAGER_BLOOD_DRAIN;
        if (VampireClothingUtil.isDrinkBoostActive(vampire)) {
            desired = (desired * 3 + 1) / 2;
        }
        return guard.takeBlood(desired, vampire);
    }

    private static int drainVillagerBlood(ServerPlayer vampire, Villager villager) {
        int desired = VILLAGER_BLOOD_DRAIN;
        if (VampireClothingUtil.isDrinkBoostActive(vampire)) {
            desired = (desired * 3 + 1) / 2;
        }

        boolean knockedOut = villager.isSleeping() || isMesmerised(villager);
        if (!knockedOut) {
            desired = (int) Math.ceil(desired * 0.66F);
        }

        var tag = villager.getPersistentData();
        int stored = tag.getIntOr(VILLAGER_BLOOD_TAG, VILLAGER_MAX_BLOOD);
        int taken = Math.min(Math.max(stored, 0), desired);
        int remaining = Math.max(0, stored - taken);
        tag.putInt(VILLAGER_BLOOD_TAG, remaining);

        if (remaining < VILLAGER_LOW_BLOOD) {
            villager.hurt(vampire.damageSources().playerAttack(vampire), 1.3F);
        } else if (!knockedOut) {
            villager.hurt(vampire.damageSources().playerAttack(vampire), 0.1F);
        }
        return taken;
    }

    /** Blood power gained per bite. Source: GenericEvents.java (EntityInteract handler). */
    private static int bloodPowerGain(ServerPlayer vampire, LivingEntity target, int bloodTaken) {
        if (target instanceof Villager || target instanceof VillageGuardEntity) {
            return VampireClothingUtil.isDrinkBoostActive(vampire) ? BP_GAIN_VILLAGER_BOOST : BP_GAIN_VILLAGER_BASE;
        }
        if (target instanceof Animal) {
            // Capped at 25% of maxBloodPower — enforced by addBloodPower → setBloodPower clamp,
            // but we also cap the gain here so repeated animal bites don't overfill past 25%.
            int cap25 = (int) Math.ceil(WitcheryAttachments.get(vampire).getMaxBloodPower() * 0.25F);
            int current = WitcheryAttachments.get(vampire).getBloodPower();
            return Math.max(0, Math.min(BP_GAIN_ANIMAL, cap25 - current));
        }
        if (target instanceof Monster) return 0; // undead have no blood power
        return BP_GAIN_OTHER;
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
        if (isMesmerised(target)) {
            playMesmeriseFailure(vampire);
            vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.already",
                    target.getName()), true);
            return;
        }
        if (data.getBloodPower() < MESMERISE_BLOOD_POWER_COST) {
            playMesmeriseFailure(vampire);
            vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.not_enough_blood",
                    MESMERISE_BLOOD_POWER_COST), true);
            return;
        }

        int duration = MESMERISE_BASE_DURATION;
        if (VampireClothingUtil.isMesmeriseBoostActive(vampire)) {
            duration += MESMERISE_BOOST_DURATION;
        }

        data.spendBloodPower(MESMERISE_BLOOD_POWER_COST);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, MESMERISE_SLOWNESS_AMPLIFIER, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, true));
        vampire.level().playSound(null, target.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.success",
                target.getName(), duration / 20), true);
    }

    private static boolean isMesmerised(LivingEntity target) {
        MobEffectInstance slowness = target.getEffect(MobEffects.SLOWNESS);
        return slowness != null
                && slowness.getAmplifier() >= MESMERISE_SLOWNESS_AMPLIFIER
                && slowness.getDuration() > 20;
    }

    private static boolean canMesmerise(LivingEntity target, WitcheryPlayerData data) {
        if (data.getVampireLevel() < MESMERISE_MIN_LEVEL) return false;
        return target instanceof Villager || target instanceof Player || target instanceof VillageGuardEntity;
    }

    private static void playMesmeriseFailure(ServerPlayer vampire) {
        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 0.5F);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isInSunlight(ServerPlayer player) {
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
