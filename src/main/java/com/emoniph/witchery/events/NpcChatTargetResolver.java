package com.emoniph.witchery.events;

import com.emoniph.witchery.ai.NpcCommandKind;
import com.emoniph.witchery.ai.NpcPromptBuilder;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.LilithEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

final class NpcChatTargetResolver {
    private static final double CHAT_RANGE = 10.0;
    private static final double CASUAL_CHAT_RANGE = 6.0;
    private static final double LEONARD_CHAT_RANGE = 12.0;
    private static final double LEONARD_CASUAL_CHAT_RANGE = 8.0;
    private static final double BABA_YAGA_CHAT_RANGE = 12.0;
    private static final double BABA_YAGA_CASUAL_CHAT_RANGE = 8.0;
    private static final double BOSS_CHAT_RANGE = 14.0;
    private static final double BOSS_CASUAL_CHAT_RANGE = 8.0;
    private static final double DEMON_CHAT_RANGE = 12.0;
    private static final double DEMON_CASUAL_CHAT_RANGE = 7.0;
    private static final double FLAME_IMP_CHAT_RANGE = 10.0;
    private static final double FLAME_IMP_CASUAL_CHAT_RANGE = 6.0;
    private static final double VILLAGER_CHAT_RANGE = 10.0;
    private static final double VILLAGER_CASUAL_CHAT_RANGE = 5.0;
    private static final double VILLAGE_GUARD_CHAT_RANGE = 12.0;
    private static final double VILLAGE_GUARD_CASUAL_CHAT_RANGE = 7.0;
    private static final double HOBGOBLIN_CHAT_RANGE = 10.0;
    private static final double HOBGOBLIN_CASUAL_CHAT_RANGE = 6.0;
    private static final double REFLECTION_CHAT_RANGE = 12.0;
    private static final double REFLECTION_CASUAL_CHAT_RANGE = 8.0;
    private static final List<PersonaTargetRule<? extends LivingEntity>> TARGET_RULES = List.of(
            new PersonaTargetRule<>(ChatPersona.COVEN_WITCH, CovenWitchEntity.class,
                    CHAT_RANGE, 4.0, CASUAL_CHAT_RANGE, NpcChatTargetResolver::isNamedAddress,
                    CasualRouting.CLOSE_PRIVATE_THEN_LOOKING_PUBLIC),
            new PersonaTargetRule<>(ChatPersona.LEONARD, LeonardEntity.class,
                    LEONARD_CHAT_RANGE, 6.0, LEONARD_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedLeonardAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BABA_YAGA, BabaYagaEntity.class,
                    BABA_YAGA_CHAT_RANGE, 6.0, BABA_YAGA_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedBabaYagaAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BOSS, DeathEntity.class,
                    BOSS_CHAT_RANGE, 7.0, BOSS_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedDeathAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BOSS, LordOfTormentEntity.class,
                    BOSS_CHAT_RANGE, 7.0, BOSS_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedLordOfTormentAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BOSS, HornedHuntsmanEntity.class,
                    BOSS_CHAT_RANGE, 7.0, BOSS_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedHornedHuntsmanAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BOSS, LilithEntity.class,
                    BOSS_CHAT_RANGE, 7.0, BOSS_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedLilithAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BOSS, GoblinGulgEntity.class,
                    BOSS_CHAT_RANGE, 7.0, BOSS_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedGulgAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.BOSS, GoblinMogEntity.class,
                    BOSS_CHAT_RANGE, 7.0, BOSS_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedMogAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.DEMON, DemonEntity.class,
                    DEMON_CHAT_RANGE, 6.0, DEMON_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedDemonAddress,
                    CasualRouting.LOOKING_CLOSE_CONVERSATIONAL),
            new PersonaTargetRule<>(ChatPersona.FLAME_IMP, FlameImpEntity.class,
                    FLAME_IMP_CHAT_RANGE, 4.0, FLAME_IMP_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedFlameImpAddress,
                    CasualRouting.CLOSE_PRIVATE_THEN_LOOKING_PUBLIC),
            new PersonaTargetRule<>(ChatPersona.VILLAGER, Villager.class,
                    VILLAGER_CHAT_RANGE, 4.0, VILLAGER_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedVillagerAddress,
                    CasualRouting.CLOSE_PRIVATE_THEN_LOOKING_PUBLIC),
            new PersonaTargetRule<>(ChatPersona.VILLAGE_GUARD, VillageGuardEntity.class,
                    VILLAGE_GUARD_CHAT_RANGE, 5.0, VILLAGE_GUARD_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedVillageGuardAddress,
                    CasualRouting.CLOSE_PRIVATE_THEN_LOOKING_PUBLIC),
            new PersonaTargetRule<>(ChatPersona.HOBGOBLIN, HobgoblinEntity.class,
                    HOBGOBLIN_CHAT_RANGE, 4.0, HOBGOBLIN_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedHobgoblinAddress,
                    CasualRouting.CLOSE_PRIVATE_THEN_LOOKING_PUBLIC),
            new PersonaTargetRule<>(ChatPersona.REFLECTION, ReflectionEntity.class,
                    REFLECTION_CHAT_RANGE, 6.0, REFLECTION_CASUAL_CHAT_RANGE,
                    NpcChatTargetResolver::isNamedReflectionAddress,
                    CasualRouting.REFLECTION)
    );

    private NpcChatTargetResolver() {
    }

    static ChatTarget findNearest(ServerLevel level, ServerPlayer player, String message) {
        List<ChatTarget> candidates = new ArrayList<>();
        EnumSet<ChatPersona> namedPersonas = explicitlyNamedPersonas(message);
        for (PersonaTargetRule<? extends LivingEntity> rule : TARGET_RULES) {
            addRuleCandidates(level, player, message, namedPersonas, rule, candidates);
        }
        return candidates.stream()
                .min(Comparator.comparingDouble(ChatTarget::distanceSqr))
                .orElse(null);
    }

    private static <E extends LivingEntity> void addRuleCandidates(ServerLevel level, ServerPlayer player,
                                                                   String message,
                                                                   EnumSet<ChatPersona> namedPersonas,
                                                                   PersonaTargetRule<E> rule,
                                                                   List<ChatTarget> candidates) {
        List<E> entities = level.getEntitiesOfClass(rule.entityClass(),
                player.getBoundingBox().inflate(rule.chatRange(), rule.verticalRange(), rule.chatRange()));
        if (entities.isEmpty()) {
            return;
        }
        entities.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (E entity : entities) {
            if (rule.namedAddress().matches(entity, message)) {
                candidates.add(new ChatTarget(rule.kind(), entity, player.distanceToSqr(entity), true));
            }
        }
        if (namesOtherPersona(namedPersonas, rule.kind())) {
            return;
        }
        E nearest = entities.getFirst();
        double distance = player.distanceToSqr(nearest);
        addCasualCandidate(level, player, message, rule, nearest, distance, candidates);
    }

    private static <E extends LivingEntity> void addCasualCandidate(ServerLevel level, ServerPlayer player,
                                                                    String message, PersonaTargetRule<E> rule,
                                                                    E nearest, double distance,
                                                                    List<ChatTarget> candidates) {
        switch (rule.casualRouting()) {
            case CLOSE_PRIVATE_THEN_LOOKING_PUBLIC -> {
                if (distance <= squared(rule.casualRange())
                        && (isCasualAddress(message) || isShortFollowUp(message))) {
                    candidates.add(new ChatTarget(rule.kind(), nearest, distance, true));
                    return;
                }
                if (distance <= squared(rule.chatRange())
                        && isLookingAt(player, nearest)
                        && isConversational(message)) {
                    candidates.add(new ChatTarget(rule.kind(), nearest, distance, false));
                }
            }
            case LOOKING_CLOSE_CONVERSATIONAL -> {
                if (distance <= squared(rule.casualRange())
                        && isLookingAt(player, nearest)
                        && isConversational(message)) {
                    boolean privateAddress = isCasualAddress(message) || isShortFollowUp(message);
                    candidates.add(new ChatTarget(rule.kind(), nearest, distance, privateAddress));
                }
            }
            case REFLECTION -> {
                if (distance <= squared(rule.casualRange())
                        && isLookingAt(player, nearest)
                        && (isCasualAddress(message)
                        || isShortFollowUp(message)
                        || WitcheryDimensions.isMirrorWorld(level))) {
                    candidates.add(new ChatTarget(rule.kind(), nearest, distance, true));
                    return;
                }
                if (distance <= squared(rule.chatRange())
                        && isLookingAt(player, nearest)
                        && isConversational(message)) {
                    candidates.add(new ChatTarget(rule.kind(), nearest, distance, false));
                }
            }
        }
    }

    private static boolean namesOtherPersona(EnumSet<ChatPersona> namedPersonas, ChatPersona kind) {
        for (ChatPersona named : namedPersonas) {
            if (named != kind) {
                return true;
            }
        }
        return false;
    }

    private static double squared(double value) {
        return value * value;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static EnumSet<ChatPersona> explicitlyNamedPersonas(String message) {
        EnumSet<ChatPersona> personas = EnumSet.noneOf(ChatPersona.class);
        if (explicitlyNamesWitch(message)) {
            personas.add(ChatPersona.COVEN_WITCH);
        }
        if (explicitlyNamesLeonard(message)) {
            personas.add(ChatPersona.LEONARD);
        }
        if (explicitlyNamesBabaYaga(message)) {
            personas.add(ChatPersona.BABA_YAGA);
        }
        if (explicitlyNamesBoss(message)) {
            personas.add(ChatPersona.BOSS);
        }
        if (explicitlyNamesDemon(message)) {
            personas.add(ChatPersona.DEMON);
        }
        if (explicitlyNamesFlameImp(message)) {
            personas.add(ChatPersona.FLAME_IMP);
        }
        if (explicitlyNamesVillager(message)) {
            personas.add(ChatPersona.VILLAGER);
        }
        if (explicitlyNamesVillageGuard(message)) {
            personas.add(ChatPersona.VILLAGE_GUARD);
        }
        if (explicitlyNamesHobgoblin(message)) {
            personas.add(ChatPersona.HOBGOBLIN);
        }
        if (explicitlyNamesReflection(message)) {
            personas.add(ChatPersona.REFLECTION);
        }
        return personas;
    }

    private static boolean isNamedAddress(CovenWitchEntity witch, String message) {
        String normalized = normalize(message);
        if (normalized.contains("witch")) {
            return true;
        }
        return matchesCustomName(witch, normalized);
    }

    private static boolean isNamedLeonardAddress(LeonardEntity leonard, String message) {
        String normalized = normalize(message);
        if (normalized.contains("leonard") || normalized.contains("shade")) {
            return true;
        }
        return matchesCustomName(leonard, normalized);
    }

    private static boolean isNamedBabaYagaAddress(BabaYagaEntity baba, String message) {
        String normalized = normalize(message);
        if (normalized.contains("baba yaga") || normalized.contains("baba") || normalized.contains("yaga")) {
            return true;
        }
        return matchesCustomName(baba, normalized);
    }

    private static boolean isNamedDeathAddress(DeathEntity death, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "death", "binky", "hand of death", "summon death", "boss")) {
            return true;
        }
        return matchesCustomName(death, normalized);
    }

    private static boolean isNamedLordOfTormentAddress(LordOfTormentEntity boss, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "lord of torment", "torment", "torment scroll", "boss")) {
            return true;
        }
        return matchesCustomName(boss, normalized);
    }

    private static boolean isNamedHornedHuntsmanAddress(HornedHuntsmanEntity boss, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "horned huntsman", "huntsman", "horned hunter", "wild hunt", "boss")) {
            return true;
        }
        return matchesCustomName(boss, normalized);
    }

    private static boolean isNamedLilithAddress(LilithEntity lilith, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "lilith", "vampire lilith", "blood queen", "boss")) {
            return true;
        }
        return matchesCustomName(lilith, normalized);
    }

    private static boolean isNamedGulgAddress(GoblinGulgEntity gulg, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "gulg", "goblin gulg", "gulg boss", "boss")) {
            return true;
        }
        return matchesCustomName(gulg, normalized);
    }

    private static boolean isNamedMogAddress(GoblinMogEntity mog, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "mog", "goblin mog", "mog boss", "boss")) {
            return true;
        }
        return matchesCustomName(mog, normalized);
    }

    private static boolean isNamedDemonAddress(DemonEntity demon, String message) {
        String normalized = normalize(message);
        if (explicitlyNamesDemon(message)) {
            return true;
        }
        return matchesCustomName(demon, normalized);
    }

    private static boolean isNamedFlameImpAddress(FlameImpEntity imp, String message) {
        String normalized = normalize(message);
        if (explicitlyNamesFlameImp(message)) {
            return true;
        }
        return matchesCustomName(imp, normalized);
    }

    private static boolean isNamedVillagerAddress(Villager villager, String message) {
        String normalized = normalize(message);
        if (normalized.contains("villager")) {
            return true;
        }
        if (matchesCustomName(villager, normalized)) {
            return true;
        }
        String profession = NpcPromptBuilder.villagerProfessionName(villager);
        return !profession.isBlank() && normalized.contains(normalize(profession));
    }

    private static boolean isNamedVillageGuardAddress(VillageGuardEntity guard, String message) {
        String normalized = normalize(message);
        if (explicitlyNamesVillageGuard(message)) {
            return true;
        }
        return matchesCustomName(guard, normalized);
    }

    private static boolean isNamedHobgoblinAddress(HobgoblinEntity hobgoblin, String message) {
        String normalized = normalize(message);
        if (normalized.contains("hobgoblin")
                || normalized.contains("koboldite")
                || normalized.contains("patron")
                || normalized.contains("goblin altar")
                || NpcCommandKind.MINE.matchesCandidate(normalized)
                || NpcCommandKind.WORSHIP.matchesCandidate(normalized)) {
            return true;
        }
        return matchesCustomName(hobgoblin, normalized);
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
        return matchesCustomName(reflection, normalized);
    }

    private static boolean matchesCustomName(LivingEntity entity, String normalizedMessage) {
        String name = entity.hasCustomName() && entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalizedName.isBlank()) {
            return false;
        }
        if (normalizedMessage.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalizedMessage.contains(first);
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

    private static boolean explicitlyNamesBoss(String message) {
        String normalized = normalize(message);
        return containsAny(normalized,
                "death", "binky", "hand of death", "summon death",
                "lord of torment", "torment scroll",
                "horned huntsman", "huntsman", "horned hunter", "wild hunt",
                "lilith", "vampire lilith",
                "goblin gulg", "goblin mog", "gulg", "mog",
                "boss");
    }

    private static boolean explicitlyNamesDemon(String message) {
        String normalized = normalize(message);
        if (explicitlyNamesFlameImp(message)) {
            return false;
        }
        return containsAny(normalized,
                "demon", "summoned demon", "devil", "devils tongue", "devil s tongue",
                "devil's tongue", "infernal blood", "demon heart", "demon bargain");
    }

    private static boolean explicitlyNamesFlameImp(String message) {
        String normalized = normalize(message);
        return containsAny(normalized,
                "flame imp", "little demon", "demonic contract", "spell contract",
                "imp magic", "please an imp", "shiny gift", "shiny gifts")
                || normalized.matches(".*(^|\\s)imp($|\\s).*");
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

    private static boolean explicitlyNamesVillageGuard(String message) {
        String normalized = normalize(message);
        return containsAny(normalized,
                "village guard", "village guards", "town guard", "town guards",
                "guard tower", "guard post", "guard patrol", "patrol guard",
                "any guards", "guards nearby", "guard nearby",
                "guard shouted", "guards shouted", "guard shouting", "guards shouting",
                "hey guard", "hello guard", "hail guard", "guard state your business",
                "guard what is your business", "guard what do you watch");
    }

    private static boolean explicitlyNamesHobgoblin(String message) {
        String normalized = normalize(message);
        return normalized.contains("hobgoblin")
                || normalized.contains("koboldite")
                || normalized.contains("patron")
                || normalized.contains("goblin altar")
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

    private enum CasualRouting {
        CLOSE_PRIVATE_THEN_LOOKING_PUBLIC,
        LOOKING_CLOSE_CONVERSATIONAL,
        REFLECTION
    }

    @FunctionalInterface
    private interface NamedAddress<E extends LivingEntity> {
        boolean matches(E entity, String message);
    }

    private record PersonaTargetRule<E extends LivingEntity>(
            ChatPersona kind,
            Class<E> entityClass,
            double chatRange,
            double verticalRange,
            double casualRange,
            NamedAddress<E> namedAddress,
            CasualRouting casualRouting) {
    }
}
