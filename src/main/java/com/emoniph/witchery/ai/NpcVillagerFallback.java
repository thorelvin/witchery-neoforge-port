package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.article;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksDemonVillagerOpinion;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksGoodBadWitches;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksMemory;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksNpcName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksPlayerName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksTaglockOrHairSafety;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksVillagerHarvest;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksVillagerWeather;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksWitchDebtOrRespect;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksWitchTrust;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsAny;
import static com.emoniph.witchery.ai.NpcDialogueFallback.countAfter;
import static com.emoniph.witchery.ai.NpcDialogueFallback.countWord;
import static com.emoniph.witchery.ai.NpcDialogueFallback.isGreeting;
import static com.emoniph.witchery.ai.NpcDialogueFallback.memoryAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.normalize;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeDisplayName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villageFactSummary;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villagerHarvestAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villagerJobAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villagerRoutedFallback;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villagerTradeAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villagerWeatherAnswer;

final class NpcVillagerFallback {
    private NpcVillagerFallback() {
    }

    static Optional<String> chat(String playerMessage, String villagerName, String playerName,
                                 String profession, String villageInfo, String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String villager = safeDisplayName(villagerName, "Villager");
        String job = safeDisplayName(profession, "villager").toLowerCase(Locale.ROOT);
        String facts = NpcBrainResponse.sanitize(villageInfo, 320);
        if (asksNpcName(normalized)) {
            return Optional.of("I'm " + villager + ", " + article(job) + " " + job
                    + " here; neighbors know me, and so does the bell.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("You're " + name + ". Folk say names carefully when witches are near.");
        }
        if (normalized.startsWith("remember ") && containsAny(normalized, "afraid", "fear", "nervous",
                "do not trust", "don't trust", "dont trust")) {
            if (containsAny(normalized, "vampire", "vampires")) {
                return Optional.of("I'll remember, " + name
                        + ": vampires frighten you. Sensible fear keeps blood inside.");
            }
            return Optional.of("I'll remember that, " + name
                    + ". Villages survive by keeping small fears in order.");
        }
        if (containsAny(normalized, "goodbye", "farewell", "bye")) {
            return Optional.of("Go careful, " + name
                    + ". Doors shut, eyes open, and bread in your pocket after dusk.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("Morning, " + name + ". Crows sat wrong on the fence; mind the road today.");
        }
        if (containsAny(normalized, "live in this village", "do you live here", "live here")) {
            return Optional.of("I live here, trade here, and worry here. That is village life in three knots.");
        }
        if (asksVillagerHarvest(normalized)) {
            return Optional.of(villagerHarvestAnswer(normalized, name, job));
        }
        if (asksVillagerWeather(normalized)) {
            return Optional.of(villagerWeatherAnswer(normalized, name));
        }
        if (containsAny(normalized, "what do you do", "your job", "profession", "work here", "who are you")) {
            return Optional.of(villagerJobAnswer(job));
        }
        if (containsAny(normalized, "primer", "witches primer", "read a primer", "leave that to witches")) {
            return Optional.of("Read it if you must, " + name
                    + ", but keep common sense beside it; books do not scream until too late.");
        }
        if (containsAny(normalized, "apothecaries coven witches", "apothecary coven witch",
                "are apothecaries witches", "are apothecaries coven")) {
            return Optional.of("Apothecaries are village trade, not coven witches; we keep herbs and remedies, not circle secrets.");
        }
        if (containsAny(normalized, "teach me witchcraft", "teach witchcraft", "learn witchcraft",
                "can you teach me magic", "teach me magic")) {
            return Optional.of("I know herb names and old warnings. For witchcraft, find a Primer or a witch with patience.");
        }
        if (containsAny(normalized, "whiff", "whif", "mutandis", "witchery recipe", "witchery recipes")) {
            return Optional.of("I've heard those names over counters. Exact witch work belongs to a Primer or a proper witch.");
        }
        if (containsAny(normalized, "how big", "village size", "how many villagers", "population",
                "how many people", "beds", "doors")) {
            return Optional.of("Nearby I count " + villageFactSummary(facts)
                    + ". Count doors before you count luck.");
        }
        if (containsAny(normalized, "golem", "iron golem", "protector")) {
            int golems = countAfter(facts, "iron golems");
            if (golems <= 0) {
                return Optional.of("No iron golem close enough for comfort. Shut doors early, " + name + ".");
            }
            return Optional.of(countWord(golems) + " iron golem" + (golems == 1 ? " is" : "s are")
                    + " nearby. Good iron, though iron sleeps slower than trouble walks.");
        }
        if (containsAny(normalized, "wall", "walls", "fence", "fences", "protected", "safe")) {
            int barriers = countAfter(facts, "walls/fences");
            if (barriers >= 24) {
                return Optional.of("Walls and fences should slow trouble. Still latch the door; night knows how to climb.");
            }
            if (barriers > 0) {
                return Optional.of("Some walls and fences, yes, but I would not wager a child's sleep on them.");
            }
            return Optional.of("I see no proper wall nearby. Doors, golems, and daylight will have to do.");
        }
        if (containsAny(normalized, "raid", "raider", "raiders", "pillager", "pillagers", "bell")) {
            if (containsAny(normalized, "ring the bell", "if i ring", "when i ring", "what happens")) {
                return Optional.of("Ring the bell for danger; villagers run for doors, and brave fools look for trouble.");
            }
            if (facts.contains("raid active")) {
                return Optional.of("Raid's active. Find a door, find the bell, and stop asking me like I enjoy it.");
            }
            int raiders = countAfter(facts, "raiders");
            if (raiders > 0) {
                return Optional.of("No proper raid bell, but " + countWord(raiders)
                        + " raider" + (raiders == 1 ? "" : "s") + " nearby is bad enough.");
            }
            return Optional.of("No raid I can see. That is not the same as safe.");
        }
        if (containsAny(normalized, "vampire", "vampires")) {
            int vampires = countAfter(facts, "vampires");
            if (vampires > 0) {
                return Optional.of(countWord(vampires) + " vampire" + (vampires == 1 ? "" : "s")
                        + " nearby. Garlic on the sill, no invitations after sunset.");
            }
            return Optional.of("No vampire nearby by my count, but pretty manners after sunset still worry me.");
        }
        if (containsAny(normalized, "werewolf", "werewolves", "wolfman", "wolfmen")) {
            int werewolves = countAfter(facts, "werewolves");
            if (werewolves > 0) {
                return Optional.of(countWord(werewolves) + " werewolf sign" + (werewolves == 1 ? "" : "s")
                        + " nearby. Silver and closed doors, " + name + ".");
            }
            return Optional.of("No werewolf sign nearby. Keep listening after moonrise anyway.");
        }
        if (asksGoodBadWitches(normalized)) {
            return Optional.of("There are good witches and bad, " + name
                    + ". Fair trade and clean cures speak well; hidden hair bottles do not.");
        }
        if (asksWitchDebtOrRespect(normalized)) {
            return Optional.of("A witch can earn respect, " + name
                    + ". Cure a child or turn aside a curse, and folk remember the debt with both hands shaking.");
        }
        if (asksWitchTrust(normalized)) {
            return Optional.of("Trust deeds, not hats. Fair trade, clean cures, and kept promises speak better than moonlit smiles.");
        }
        if (asksTaglockOrHairSafety(normalized)) {
            return Optional.of("Keep hair, names, and taglocks close, " + name
                    + "; what leaves your hand may learn the wrong road home.");
        }
        if (containsAny(normalized, "witch", "witches", "coven", "curse", "taglock", "poppet")) {
            int witches = countAfter(facts, "witches");
            if (witches > 0) {
                return Optional.of("Witches nearby make folk count their hair twice. Keep your name out of bottles.");
            }
            return Optional.of("No witch close by that I can see. Still, never leave hair where strangers sweep.");
        }
        if (containsAny(normalized, "guard", "guards")) {
            int guards = countAfter(facts, "village guards");
            if (guards > 0) {
                return Optional.of(countWord(guards) + " guard" + (guards == 1 ? " is" : "s are")
                        + " nearby. Good for noise, better with a golem.");
            }
            return Optional.of("No village guard near us. If trouble comes, the bell will do the shouting.");
        }
        if (containsAny(normalized, "trade", "sell", "buy", "stock", "emerald")) {
            return Optional.of(villagerTradeAnswer(job));
        }
        if (containsAny(normalized, "sleep here", "sleep tonight", "stay here tonight", "safe to sleep")) {
            return Optional.of("Sleep only with a bed, a door, and no bell screaming. Salt the threshold if you can.");
        }
        if (containsAny(normalized, "who watches the roads", "watch roads", "road after sunset", "roads after sunset")) {
            return Optional.of("After sunset? Guards, golems, and fools with lanterns. Best be behind a door before then.");
        }
        if (containsAny(normalized, "before dusk", "what should i do before dusk")) {
            return Optional.of("Before dusk, shut doors, count bread, check the bell, and keep your name out of bargains.");
        }
        if (containsAny(normalized, "demon comes", "demon down the road", "if a demon", "demon on the road")) {
            return Optional.of("If a demon comes down the road, shut doors, find the bell, and let braver fools stand outside.");
        }
        if (asksDemonVillagerOpinion(normalized)) {
            return Optional.of("Demons? Worse than witches. Witches may trade; demons bargain until the debt has teeth.");
        }
        if (containsAny(normalized, "bring on the road", "besides a sword", "road besides a sword",
                "what should i bring on the road")) {
            return Optional.of("Bring bread, garlic, dry boots, and coin hidden twice. A sword only answers the loudest trouble.");
        }
        if (containsAny(normalized, "crows", "crow")) {
            return Optional.of("Crows mean something when they gather quiet. One crow is weather; three are gossip with wings.");
        }
        if (containsAny(normalized, "milk turns sour", "sour milk", "milk sour")) {
            return Optional.of("Sour milk first blames heat, then bad luck, then the neighbor nobody trusts.");
        }
        if (containsAny(normalized, "rumor", "rumour", "gossip")) {
            return Optional.of("Rumor says the old well repeats names after rain. I have not leaned close enough to argue.");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I know your name, " + name + ", and not much else fit for village gossip."));
        }
        Optional<String> routed = villagerRoutedFallback(normalized, name, job, facts, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        return Optional.of(NpcFallbackAsides.villager(normalized, name, ""));
    }
}
