package com.emoniph.witchery.events;

import com.emoniph.witchery.ai.NpcCommandKind;
import com.emoniph.witchery.ai.NpcPromptBuilder;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class NpcChatTargetResolver {
    private static final double CHAT_RANGE = 10.0;
    private static final double CASUAL_CHAT_RANGE = 6.0;
    private static final double LEONARD_CHAT_RANGE = 12.0;
    private static final double LEONARD_CASUAL_CHAT_RANGE = 8.0;
    private static final double BABA_YAGA_CHAT_RANGE = 12.0;
    private static final double BABA_YAGA_CASUAL_CHAT_RANGE = 8.0;
    private static final double VILLAGER_CHAT_RANGE = 10.0;
    private static final double VILLAGER_CASUAL_CHAT_RANGE = 5.0;
    private static final double HOBGOBLIN_CHAT_RANGE = 10.0;
    private static final double HOBGOBLIN_CASUAL_CHAT_RANGE = 6.0;
    private static final double REFLECTION_CHAT_RANGE = 12.0;
    private static final double REFLECTION_CASUAL_CHAT_RANGE = 8.0;

    private NpcChatTargetResolver() {
    }

    static ChatTarget findNearest(ServerLevel level, ServerPlayer player, String message) {
        List<ChatTarget> candidates = new ArrayList<>();
        addCovenChatCandidates(level, player, message, candidates);
        addLeonardChatCandidates(level, player, message, candidates);
        addBabaYagaChatCandidates(level, player, message, candidates);
        addVillagerChatCandidates(level, player, message, candidates);
        addHobgoblinChatCandidates(level, player, message, candidates);
        addReflectionChatCandidates(level, player, message, candidates);
        return candidates.stream()
                .min(Comparator.comparingDouble(ChatTarget::distanceSqr))
                .orElse(null);
    }

    private static void addCovenChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                               List<ChatTarget> candidates) {
        List<CovenWitchEntity> witches = level.getEntitiesOfClass(
                CovenWitchEntity.class, player.getBoundingBox().inflate(CHAT_RANGE, 4.0, CHAT_RANGE));
        if (witches.isEmpty()) {
            return;
        }
        witches.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesLeonard(message) || explicitlyNamesBabaYaga(message)
                || explicitlyNamesVillager(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
        for (CovenWitchEntity witch : witches) {
            if (isNamedAddress(witch, message)) {
                candidates.add(new ChatTarget(ChatPersona.COVEN_WITCH, witch, player.distanceToSqr(witch), true));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        CovenWitchEntity nearest = witches.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= CASUAL_CHAT_RANGE * CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            candidates.add(new ChatTarget(ChatPersona.COVEN_WITCH, nearest, distance, true));
            return;
        }
        if (distance <= CHAT_RANGE * CHAT_RANGE && isLookingAt(player, nearest) && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.COVEN_WITCH, nearest, distance, false));
        }
    }

    private static void addLeonardChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                 List<ChatTarget> candidates) {
        List<LeonardEntity> shades = level.getEntitiesOfClass(
                LeonardEntity.class, player.getBoundingBox().inflate(LEONARD_CHAT_RANGE, 6.0, LEONARD_CHAT_RANGE));
        if (shades.isEmpty()) {
            return;
        }
        shades.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesBabaYaga(message)
                || explicitlyNamesVillager(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
        for (LeonardEntity shade : shades) {
            if (isNamedLeonardAddress(shade, message)) {
                candidates.add(new ChatTarget(ChatPersona.LEONARD, shade, player.distanceToSqr(shade), true));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        LeonardEntity nearest = shades.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= LEONARD_CASUAL_CHAT_RANGE * LEONARD_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && isConversational(message)) {
            boolean privateAddress = isCasualAddress(message) || isShortFollowUp(message);
            candidates.add(new ChatTarget(ChatPersona.LEONARD, nearest, distance, privateAddress));
        }
    }

    private static void addBabaYagaChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                  List<ChatTarget> candidates) {
        List<BabaYagaEntity> crones = level.getEntitiesOfClass(
                BabaYagaEntity.class, player.getBoundingBox().inflate(BABA_YAGA_CHAT_RANGE, 6.0, BABA_YAGA_CHAT_RANGE));
        if (crones.isEmpty()) {
            return;
        }
        crones.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesVillager(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
        for (BabaYagaEntity crone : crones) {
            if (isNamedBabaYagaAddress(crone, message)) {
                candidates.add(new ChatTarget(ChatPersona.BABA_YAGA, crone, player.distanceToSqr(crone), true));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        BabaYagaEntity nearest = crones.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= BABA_YAGA_CASUAL_CHAT_RANGE * BABA_YAGA_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && isConversational(message)) {
            boolean privateAddress = isCasualAddress(message) || isShortFollowUp(message);
            candidates.add(new ChatTarget(ChatPersona.BABA_YAGA, nearest, distance, privateAddress));
        }
    }

    private static void addVillagerChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                  List<ChatTarget> candidates) {
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(VILLAGER_CHAT_RANGE, 4.0, VILLAGER_CHAT_RANGE));
        if (villagers.isEmpty()) {
            return;
        }
        villagers.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesBabaYaga(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
        for (Villager villager : villagers) {
            if (isNamedVillagerAddress(villager, message)) {
                candidates.add(new ChatTarget(ChatPersona.VILLAGER, villager, player.distanceToSqr(villager), true));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        Villager nearest = villagers.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= VILLAGER_CASUAL_CHAT_RANGE * VILLAGER_CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            candidates.add(new ChatTarget(ChatPersona.VILLAGER, nearest, distance, true));
            return;
        }
        if (distance <= VILLAGER_CHAT_RANGE * VILLAGER_CHAT_RANGE && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.VILLAGER, nearest, distance, false));
        }
    }

    private static void addHobgoblinChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                   List<ChatTarget> candidates) {
        List<HobgoblinEntity> hobgoblins = level.getEntitiesOfClass(
                HobgoblinEntity.class, player.getBoundingBox().inflate(HOBGOBLIN_CHAT_RANGE, 4.0, HOBGOBLIN_CHAT_RANGE));
        if (hobgoblins.isEmpty()) {
            return;
        }
        hobgoblins.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesBabaYaga(message) || explicitlyNamesVillager(message)
                || explicitlyNamesReflection(message);
        for (HobgoblinEntity hobgoblin : hobgoblins) {
            if (isNamedHobgoblinAddress(hobgoblin, message)) {
                candidates.add(new ChatTarget(ChatPersona.HOBGOBLIN, hobgoblin, player.distanceToSqr(hobgoblin), true));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        HobgoblinEntity nearest = hobgoblins.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= HOBGOBLIN_CASUAL_CHAT_RANGE * HOBGOBLIN_CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            candidates.add(new ChatTarget(ChatPersona.HOBGOBLIN, nearest, distance, true));
            return;
        }
        if (distance <= HOBGOBLIN_CHAT_RANGE * HOBGOBLIN_CHAT_RANGE && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.HOBGOBLIN, nearest, distance, false));
        }
    }

    private static void addReflectionChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                    List<ChatTarget> candidates) {
        List<ReflectionEntity> reflections = level.getEntitiesOfClass(
                ReflectionEntity.class, player.getBoundingBox().inflate(REFLECTION_CHAT_RANGE, 6.0, REFLECTION_CHAT_RANGE));
        if (reflections.isEmpty()) {
            return;
        }
        reflections.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesBabaYaga(message) || explicitlyNamesVillager(message)
                || explicitlyNamesHobgoblin(message);
        for (ReflectionEntity reflection : reflections) {
            if (isNamedReflectionAddress(reflection, message)) {
                candidates.add(new ChatTarget(ChatPersona.REFLECTION, reflection, player.distanceToSqr(reflection), true));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        ReflectionEntity nearest = reflections.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= REFLECTION_CASUAL_CHAT_RANGE * REFLECTION_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && (isCasualAddress(message) || isShortFollowUp(message) || WitcheryDimensions.isMirrorWorld(level))) {
            candidates.add(new ChatTarget(ChatPersona.REFLECTION, nearest, distance, true));
            return;
        }
        if (distance <= REFLECTION_CHAT_RANGE * REFLECTION_CHAT_RANGE && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.REFLECTION, nearest, distance, false));
        }
    }

    private static boolean isNamedAddress(CovenWitchEntity witch, String message) {
        String normalized = normalize(message);
        if (normalized.contains("witch")) {
            return true;
        }
        String name = witch.hasCustomName() ? witch.getCustomName().getString() : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean isNamedLeonardAddress(LeonardEntity leonard, String message) {
        String normalized = normalize(message);
        if (normalized.contains("leonard") || normalized.contains("shade")) {
            return true;
        }
        String name = leonard.hasCustomName() ? leonard.getCustomName().getString() : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean isNamedBabaYagaAddress(BabaYagaEntity baba, String message) {
        String normalized = normalize(message);
        if (normalized.contains("baba yaga") || normalized.contains("baba") || normalized.contains("yaga")) {
            return true;
        }
        String name = baba.hasCustomName() && baba.getCustomName() != null
                ? baba.getCustomName().getString()
                : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean isNamedVillagerAddress(Villager villager, String message) {
        String normalized = normalize(message);
        if (normalized.contains("villager")) {
            return true;
        }
        String name = villager.hasCustomName() && villager.getCustomName() != null
                ? villager.getCustomName().getString()
                : "";
        if (!name.isBlank()) {
            String normalizedName = normalize(name);
            if (normalized.contains(normalizedName)) {
                return true;
            }
            String first = normalizedName.split("\\s+")[0];
            if (first.length() >= 3 && normalized.contains(first)) {
                return true;
            }
        }
        String profession = NpcPromptBuilder.villagerProfessionName(villager);
        return !profession.isBlank() && normalized.contains(normalize(profession));
    }

    private static boolean isNamedHobgoblinAddress(HobgoblinEntity hobgoblin, String message) {
        String normalized = normalize(message);
        if (normalized.contains("hobgoblin")
                || normalized.contains("koboldite")
                || normalized.contains("patron")
                || normalized.contains("goblin altar")
                || normalized.contains("mog")
                || normalized.contains("gulg")
                || NpcCommandKind.MINE.matchesCandidate(normalized)
                || NpcCommandKind.WORSHIP.matchesCandidate(normalized)) {
            return true;
        }
        String name = hobgoblin.hasCustomName() && hobgoblin.getCustomName() != null
                ? hobgoblin.getCustomName().getString()
                : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean isNamedReflectionAddress(ReflectionEntity reflection, String message) {
        String normalized = normalize(message);
        if (explicitlyNamesReflection(message)) {
            return true;
        }
        String ownerName = reflection.getOwnerName();
        if (ownerName != null && !ownerName.isBlank()) {
            String owner = normalize(ownerName);
            if (normalized.contains(owner + " reflection")
                    || normalized.contains(owner + "s reflection")
                    || normalized.contains("my reflection")) {
                return true;
            }
        }
        String name = reflection.hasCustomName() && reflection.getCustomName() != null
                ? reflection.getCustomName().getString()
                : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean explicitlyNamesWitch(String message) {
        String normalized = normalize(message);
        return normalized.contains("witch") || normalized.contains("coven");
    }

    private static boolean explicitlyNamesLeonard(String message) {
        String normalized = normalize(message);
        return normalized.contains("leonard") || normalized.contains("shade of leonard")
                || normalized.contains("the shade");
    }

    private static boolean explicitlyNamesBabaYaga(String message) {
        String normalized = normalize(message);
        return normalized.contains("baba yaga") || normalized.contains("baba") || normalized.contains("yaga");
    }

    private static boolean explicitlyNamesVillager(String message) {
        String normalized = normalize(message);
        return normalized.contains("villager")
                || normalized.contains("apothecary")
                || normalized.contains("farmer")
                || normalized.contains("cleric")
                || normalized.contains("librarian")
                || normalized.contains("cartographer")
                || normalized.contains("armorer")
                || normalized.contains("armourer")
                || normalized.contains("weaponsmith")
                || normalized.contains("toolsmith")
                || normalized.contains("mason")
                || normalized.contains("fletcher")
                || normalized.contains("fisherman")
                || normalized.contains("shepherd")
                || normalized.contains("butcher")
                || normalized.contains("leatherworker")
                || normalized.contains("nitwit");
    }

    private static boolean explicitlyNamesHobgoblin(String message) {
        String normalized = normalize(message);
        return normalized.contains("hobgoblin")
                || normalized.contains("koboldite")
                || normalized.contains("patron")
                || normalized.contains("goblin altar")
                || normalized.contains("mog")
                || normalized.contains("gulg")
                || NpcCommandKind.MINE.matchesCandidate(normalized)
                || NpcCommandKind.WORSHIP.matchesCandidate(normalized);
    }

    private static boolean explicitlyNamesReflection(String message) {
        String normalized = normalize(message);
        return normalized.contains("reflection")
                || normalized.contains("mirror guardian")
                || normalized.contains("mirror world")
                || normalized.contains("my echo")
                || normalized.contains("mirror mirror");
    }

    private static boolean isCasualAddress(String message) {
        String normalized = normalize(message);
        return normalized.matches("^(hi|hello|hey|hail|greetings|yo|sup)\\b.*")
                || normalized.contains("?")
                || normalized.contains("trade")
                || normalized.contains("quest")
                || normalized.contains("help");
    }

    private static boolean isConversational(String message) {
        String normalized = normalize(message);
        return normalized.length() <= 160 && !normalized.startsWith("/");
    }

    private static boolean isShortFollowUp(String message) {
        String normalized = normalize(message);
        return normalized.matches("^(how|why|what|where|when|who|yes|no|nah|ok|okay|sure|thanks|thank you)\\b.*")
                || normalized.length() <= 18;
    }

    static boolean isLookingAt(ServerPlayer player, LivingEntity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 entityCenter = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
        Vec3 offset = entityCenter.subtract(eye);
        if (offset.lengthSqr() < 1.0E-4) {
            return true;
        }
        return player.getLookAngle().normalize().dot(offset.normalize()) > 0.82 && player.hasLineOfSight(entity);
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_ ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
