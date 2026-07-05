package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.VampireEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WereVillagerEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class NpcWorldInteractionEvents {
    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final double NPC_RANGE = 10.0D;
    private static final double THREAT_RANGE = 14.0D;
    private static final double GOLEM_RANGE = 7.0D;
    private static final double PET_RANGE = 6.5D;
    private static final double PET_APPROACH_DISTANCE = 2.65D;
    private static final double SPEECH_RANGE = 10.0D;
    private static final long PLAYER_LINE_COOLDOWN_TICKS = 20L * 12L;
    private static final long THREAT_COOLDOWN_TICKS = 20L * 70L;
    private static final long SOCIAL_COOLDOWN_TICKS = 20L * 150L;
    private static final long PET_COOLDOWN_TICKS = 20L * 180L;
    private static final long PET_APPROACH_COOLDOWN_TICKS = 20L * 5L;
    private static final int COOLDOWN_PRUNE_SIZE = 512;

    private static final ConcurrentHashMap<String, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();

    private NpcWorldInteractionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || player.isSpectator()
                || !player.isAlive()
                || player.tickCount % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        long now = level.getGameTime();
        maybePrune(now);
        if (isPlayerCoolingDown(player, now)) {
            return;
        }
        if (tryThreatCue(level, player, now)) {
            return;
        }
        if (tryPetCue(level, player, now)) {
            return;
        }
        if (tryGolemCue(level, player, now)) {
            return;
        }
        tryVillageLifeCue(level, player, now);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        COOLDOWNS.clear();
        PLAYER_COOLDOWNS.clear();
    }

    private static boolean tryThreatCue(ServerLevel level, ServerPlayer player, long now) {
        ThreatCue threat = transformedPlayerCue(player);
        if (threat == null) {
            threat = nearestWorldThreat(level, player);
        }
        if (threat == null) {
            return false;
        }

        VillageGuardEntity guard = nearest(level, VillageGuardEntity.class, player, NPC_RANGE,
                guardEntity -> guardEntity.getTarget() == null || guardEntity.getTarget() == player);
        if (guard != null && markCooldown("guard-threat:" + guard.getUUID() + ":" + threat.kind(), now,
                THREAT_COOLDOWN_TICKS)) {
            return dispatchGuardAmbient(level, guard, player, threat.cue(),
                    guardThreatFallback(threat), true, now);
        }

        Villager villager = nearestEligibleVillager(level, player, NPC_RANGE);
        if (villager != null && markCooldown("villager-threat:" + villager.getUUID() + ":" + threat.kind(), now,
                THREAT_COOLDOWN_TICKS)) {
            return dispatchVillagerAmbient(level, villager, player, threat.cue(),
                    villagerThreatFallback(threat), true, now);
        }

        CovenWitchEntity witch = nearest(level, CovenWitchEntity.class, player, NPC_RANGE, witchEntity -> true);
        if (witch != null && markCooldown("witch-threat:" + witch.getUUID() + ":" + threat.kind(), now,
                THREAT_COOLDOWN_TICKS)) {
            return dispatchWitchAmbient(level, witch, player, threat.cue(), witchThreatFallback(threat), now);
        }
        return false;
    }

    private static boolean tryPetCue(ServerLevel level, ServerPlayer player, long now) {
        Villager villager = nearestEligibleVillager(level, player, NPC_RANGE);
        if (villager == null) {
            return false;
        }
        PetCue pet = nearestPet(level, villager);
        if (pet == null) {
            return false;
        }
        String pairKey = "pet:" + villager.getUUID() + ":" + pet.entity().getUUID();
        if (isCoolingDown(pairKey, now)) {
            return false;
        }

        double distance = villager.distanceTo(pet.entity());
        villager.getLookControl().setLookAt(pet.entity(), 30.0F, 30.0F);
        if (distance > PET_APPROACH_DISTANCE) {
            String approachKey = "pet-approach:" + villager.getUUID() + ":" + pet.entity().getUUID();
            if (markCooldown(approachKey, now, PET_APPROACH_COOLDOWN_TICKS)) {
                villager.getNavigation().moveTo(pet.entity(), 0.55D);
                return true;
            }
            return false;
        }

        villager.getNavigation().stop();
        level.broadcastEntityEvent(villager, (byte) 14);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.15D, villager.getZ(),
                8, 0.25D, 0.25D, 0.25D, 0.01D);
        sayVillagerToNearby(level, villager, pet.speech());
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL,
                0.45F, 1.15F);
        markCooldown(pairKey, now, PET_COOLDOWN_TICKS);
        markPlayerCooldown(player, now);
        return true;
    }

    private static boolean tryGolemCue(ServerLevel level, ServerPlayer player, long now) {
        IronGolem golem = nearest(level, IronGolem.class, player, GOLEM_RANGE, golemEntity -> true);
        if (golem == null) {
            return false;
        }

        VillageGuardEntity guard = nearest(level, VillageGuardEntity.class, player, NPC_RANGE,
                guardEntity -> guardEntity.getTarget() == null);
        if (guard != null && guard.distanceTo(golem) <= GOLEM_RANGE
                && markCooldown("guard-golem:" + guard.getUUID(), now, SOCIAL_COOLDOWN_TICKS)) {
            return dispatchGuardAmbient(level, guard, player,
                    "iron golem walking close to the guard post",
                    "Good heavy feet. I still watch the doors.", false, now);
        }

        Villager villager = nearestEligibleVillager(level, player, NPC_RANGE);
        if (villager != null && villager.distanceTo(golem) <= GOLEM_RANGE
                && markCooldown("villager-golem:" + villager.getUUID(), now, SOCIAL_COOLDOWN_TICKS)) {
            return dispatchVillagerAmbient(level, villager, player,
                    "iron golem walking close to the village",
                    "Good iron nearby. Still, a golem cannot watch every door.", false, now);
        }
        return false;
    }

    private static boolean tryVillageLifeCue(ServerLevel level, ServerPlayer player, long now) {
        Villager villager = nearestEligibleVillager(level, player, NPC_RANGE);
        if (villager == null) {
            return false;
        }

        if (isAtWorkstation(level, villager)
                && markCooldown("villager-workstation:" + villager.getUUID(), now, SOCIAL_COOLDOWN_TICKS)) {
            return dispatchVillagerAmbient(level, villager, player,
                    "working at a workstation while villagers talk about dinner and stored food",
                    "Work first, supper after. Empty barrels make folk sharp-tongued.", false, now);
        }

        if (player.tickCount % (SCAN_INTERVAL_TICKS * 4) != 0
                || !markCooldown("villager-life:" + villager.getUUID(), now, SOCIAL_COOLDOWN_TICKS)) {
            return false;
        }
        VillageLifeCue cue = villageLifeCue(level, villager);
        return dispatchVillagerAmbient(level, villager, player, cue.cue(), cue.fallback(), false, now);
    }

    private static boolean dispatchVillagerAmbient(ServerLevel level, Villager villager, ServerPlayer player,
                                                   String cue, String fallbackSpeech, boolean allowLlm, long now) {
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.VILLAGER, villager, fallbackSpeech);
        boolean queued = allowLlm && NpcBrainService.INSTANCE.requestVillagerAmbient(villager, player, cue, fallback);
        if (!queued) {
            villager.getNavigation().stop();
            villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
            player.displayClientMessage(fallback, false);
            level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL,
                    0.35F, 1.0F);
        }
        markPlayerCooldown(player, now);
        return true;
    }

    private static boolean dispatchGuardAmbient(ServerLevel level, VillageGuardEntity guard, ServerPlayer player,
                                                String cue, String fallbackSpeech, boolean allowLlm, long now) {
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.VILLAGE_GUARD, guard, fallbackSpeech);
        boolean queued = allowLlm && NpcBrainService.INSTANCE.requestVillageGuardAmbient(guard, player, cue, fallback);
        if (!queued) {
            guard.getNavigation().stop();
            guard.getLookControl().setLookAt(player, 30.0F, 30.0F);
            player.displayClientMessage(fallback, false);
            level.playSound(null, guard.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL,
                    0.4F, 0.82F);
        }
        markPlayerCooldown(player, now);
        return true;
    }

    private static boolean dispatchWitchAmbient(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                                String cue, String fallbackSpeech, long now) {
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.COVEN_WITCH, witch, fallbackSpeech);
        boolean queued = NpcBrainService.INSTANCE.requestCovenAmbient(witch, player, cue, fallback);
        if (!queued) {
            witch.getNavigation().stop();
            witch.getLookControl().setLookAt(player, 30.0F, 30.0F);
            player.displayClientMessage(fallback, false);
            level.playSound(null, witch.blockPosition(), SoundEvents.WITCH_AMBIENT, SoundSource.NEUTRAL,
                    0.35F, 1.0F);
        }
        markPlayerCooldown(player, now);
        return true;
    }

    private static Villager nearestEligibleVillager(ServerLevel level, LivingEntity center, double range) {
        return nearest(level, Villager.class, center, range, NpcWorldInteractionEvents::eligibleVillager);
    }

    private static boolean eligibleVillager(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && !villager.isSleeping()
                && villager.getTradingPlayer() == null;
    }

    private static boolean isAtWorkstation(ServerLevel level, Villager villager) {
        java.util.Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return jobSite.isPresent()
                && jobSite.get().dimension().equals(level.dimension())
                && jobSite.get().pos().distSqr(villager.blockPosition()) <= 9.0D;
    }

    private static PetCue nearestPet(ServerLevel level, Villager villager) {
        Cat catEntity = nearest(level, Cat.class, villager, PET_RANGE, LivingEntity::isAlive);
        CatFamiliarEntity familiarEntity = nearest(level, CatFamiliarEntity.class, villager, PET_RANGE,
                LivingEntity::isAlive);
        Wolf wolfEntity = nearest(level, Wolf.class, villager, PET_RANGE,
                wolf -> wolf.isAlive() && wolf.isTame());
        PetCue cat = catEntity == null
                ? null
                : new PetCue(catEntity, "What a cute kitty.");
        PetCue familiar = familiarEntity == null
                ? null
                : new PetCue(familiarEntity, "What a cute kitty.");
        PetCue wolf = wolfEntity == null
                ? null
                : new PetCue(wolfEntity, "What a good dog.");

        PetCue best = null;
        for (PetCue candidate : new PetCue[] { cat, familiar, wolf }) {
            if (candidate == null) {
                continue;
            }
            if (best == null || villager.distanceToSqr(candidate.entity()) < villager.distanceToSqr(best.entity())) {
                best = candidate;
            }
        }
        return best;
    }

    private static ThreatCue transformedPlayerCue(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.isVampire()) {
            return new ThreatCue("vampire_player", "transformed player looks like a vampire near the village");
        }
        if (data.isWerewolf()) {
            return new ThreatCue("werewolf_player", "transformed player looks like a werewolf near the village");
        }
        return null;
    }

    private static ThreatCue nearestWorldThreat(ServerLevel level, ServerPlayer player) {
        LivingEntity vampire = nearest(level, VampireEntity.class, player, THREAT_RANGE, LivingEntity::isAlive);
        LivingEntity wolfman = nearest(level, WolfmanEntity.class, player, THREAT_RANGE, LivingEntity::isAlive);
        LivingEntity wereVillager = nearest(level, WereVillagerEntity.class, player, THREAT_RANGE,
                LivingEntity::isAlive);
        LivingEntity best = null;
        String kind = "";
        String cue = "";
        if (vampire != null) {
            best = vampire;
            kind = "vampire";
            cue = "spotted vampire near the village";
        }
        if (wolfman != null && (best == null || player.distanceToSqr(wolfman) < player.distanceToSqr(best))) {
            best = wolfman;
            kind = "werewolf";
            cue = "spotted werewolf near the village";
        }
        if (wereVillager != null && (best == null || player.distanceToSqr(wereVillager) < player.distanceToSqr(best))) {
            kind = "werewolf";
            cue = "spotted were-villager near the village";
        }
        return cue.isBlank() ? null : new ThreatCue(kind, cue);
    }

    private static String guardThreatFallback(ThreatCue threat) {
        if (threat.kind().contains("vampire")) {
            return "Vampire near the doors. Keep distance and check every mouth.";
        }
        return "Werewolf near the village. Doors shut, silver ready, eyes open.";
    }

    private static String villagerThreatFallback(ThreatCue threat) {
        if (threat.kind().contains("vampire")) {
            return "Garlic by the door, now. Charming smiles bite deepest after dusk.";
        }
        return "Moon or no moon, I do not like that shape near our doors.";
    }

    private static String witchThreatFallback(ThreatCue threat) {
        if (threat.kind().contains("vampire")) {
            return "Blood folk at the road again. Keep your name and neck covered.";
        }
        return "Werewolf stink carries far. Shut the door before courage starts barking.";
    }

    private static VillageLifeCue villageLifeCue(ServerLevel level, Villager villager) {
        String profession = com.emoniph.witchery.ai.NpcPromptBuilder.villagerProfessionName(villager);
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime >= 10500L && dayTime <= 14000L) {
            return new VillageLifeCue(
                    "talking about dinner, gossip, and food stores before nightfall",
                    "Supper soon, if the barrels hold. Hungry folk gossip with sharper teeth.");
        }
        if ("farmer".equals(profession)) {
            return new VillageLifeCue(
                    "farmer thinking about grain stores, bread, and supper gossip",
                    "Grain dry, bread counted, crows watched. That is half a village kept alive.");
        }
        if ("butcher".equals(profession)) {
            return new VillageLifeCue(
                    "butcher thinking about smokehouses, salt, dinner, and hungry neighbors",
                    "Salt the meat and mind the smokehouse. Hungry neighbors hear every knife.");
        }
        return new VillageLifeCue(
                "talking about food stores, dinner gossip, and village chores",
                "Full barrels make kind gossip. Empty ones make folk count each other's plates.");
    }

    private static void sayVillagerToNearby(ServerLevel level, Villager villager, String speech) {
        Component line = NpcBrainService.speechComponent(NpcPersonas.VILLAGER, villager, speech);
        AABB area = villager.getBoundingBox().inflate(SPEECH_RANGE, 4.0D, SPEECH_RANGE);
        for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            nearby.displayClientMessage(line, false);
        }
    }

    private static <T extends LivingEntity> T nearest(ServerLevel level, Class<T> type, LivingEntity center,
                                                     double range, Predicate<T> predicate) {
        AABB bounds = center.getBoundingBox().inflate(range, 5.0D, range);
        return level.getEntitiesOfClass(type, bounds, entity -> entity.isAlive() && predicate.test(entity)).stream()
                .min(Comparator.comparingDouble(center::distanceToSqr))
                .orElse(null);
    }

    private static boolean isPlayerCoolingDown(ServerPlayer player, long now) {
        return PLAYER_COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now;
    }

    private static void markPlayerCooldown(ServerPlayer player, long now) {
        PLAYER_COOLDOWNS.put(player.getUUID(), now + PLAYER_LINE_COOLDOWN_TICKS);
    }

    private static boolean isCoolingDown(String key, long now) {
        return COOLDOWNS.getOrDefault(key, 0L) > now;
    }

    private static boolean markCooldown(String key, long now, long ticks) {
        String cleanKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
        if (cleanKey.isBlank() || isCoolingDown(cleanKey, now)) {
            return false;
        }
        COOLDOWNS.put(cleanKey, now + ticks);
        return true;
    }

    private static void maybePrune(long now) {
        if (COOLDOWNS.size() > COOLDOWN_PRUNE_SIZE) {
            COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        if (PLAYER_COOLDOWNS.size() > COOLDOWN_PRUNE_SIZE) {
            PLAYER_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
    }

    private record ThreatCue(String kind, String cue) {
    }

    private record PetCue(LivingEntity entity, String speech) {
    }

    private record VillageLifeCue(String cue, String fallback) {
    }
}
