package com.emoniph.witchery.ai;

import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.asksDemonVillagerOpinion;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksGoodBadWitches;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksMemory;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksNpcName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksPlayerName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsAny;
import static com.emoniph.witchery.ai.NpcDialogueFallback.countAfter;
import static com.emoniph.witchery.ai.NpcDialogueFallback.countWord;
import static com.emoniph.witchery.ai.NpcDialogueFallback.isGreeting;
import static com.emoniph.witchery.ai.NpcDialogueFallback.memoryAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.normalize;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeDisplayName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.villageFactSummary;

final class NpcVillageGuardFallback {
    private NpcVillageGuardFallback() {
    }

    static Optional<String> chat(String playerMessage, String guardName, String playerName,
                                 String guardState, String villageInfo, String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String guard = safeDisplayName(guardName, "Village Guard");
        String state = NpcBrainResponse.sanitize(guardState, 260);
        String facts = NpcBrainResponse.sanitize(villageInfo, 320);

        if (asksNpcName(normalized)) {
            return Optional.of("I'm " + guard + ". I watch the road, the bell, and whatever tests the gate.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("You're " + name + ". Names matter less than where your hands are.");
        }
        if (normalized.startsWith("remember ") && containsAny(normalized, "afraid", "fear", "nervous",
                "do not trust", "don't trust", "dont trust")) {
            if (containsAny(normalized, "vampire", "vampires")) {
                return Optional.of("I'll remember, " + name
                        + ": vampires frighten you. That fear keeps a pulse in your throat.");
            }
            return Optional.of("I'll remember that, " + name
                    + ". Fear belongs on watch, not loose in the street.");
        }
        if (containsAny(normalized, "goodbye", "farewell", "bye")) {
            return Optional.of("Move before dusk if you mean to move. After that, stay where doors can help.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("State your business, " + name + ". Quiet roads make me more nervous than loud ones.");
        }
        if (containsAny(normalized, "who are you", "what are you", "what do you do",
                "your job", "guard job", "guard duty")) {
            return Optional.of("I keep watch: bow at distance, sword if trouble reaches the doors.");
        }
        if (!containsAny(normalized, "vampire", "vampires", "werewolf", "werewolves", "wolfman",
                "wolfmen", "witch", "witches", "demon", "demons", "devil", "raid", "raider",
                "raiders", "pillager", "pillagers", "bell")
                && containsAny(normalized, "what do you guard", "who do you guard", "what do you watch",
                "what are you watching", "patrol", "gate", "gates", "road", "roads")) {
            return Optional.of("Roads, gates, roofs, and fools after sunset. Mostly I guard villagers from what smiles too well.");
        }
        if (containsAny(normalized, "how big", "village size", "how many villagers", "population",
                "beds", "doors")) {
            return Optional.of("From this post I count " + villageFactSummary(facts)
                    + ". Numbers change fast when the bell starts.");
        }
        if (containsAny(normalized, "guard", "guards", "how many guards", "any guards nearby")) {
            int guards = countAfter(facts, "village guards");
            if (guards <= 1) {
                return Optional.of("I am the close guard by my count. One bow still wants walls and a bell.");
            }
            return Optional.of(countWord(guards) + " guards nearby, counting me. Enough to shout, not enough to sleep.");
        }
        if (containsAny(normalized, "golem", "iron golem", "protector")) {
            int golems = countAfter(facts, "iron golems");
            if (golems <= 0) {
                return Optional.of("No iron golem close enough. So you get leather, arrows, and my bad mood.");
            }
            return Optional.of(countWord(golems) + " iron golem" + (golems == 1 ? " is" : "s are")
                    + " nearby. Good iron, but it cannot watch every door.");
        }
        if (containsAny(normalized, "wall", "walls", "fence", "fences", "protected", "safe",
                "safe here", "is this village safe")) {
            int barriers = countAfter(facts, "walls/fences");
            if (barriers >= 24) {
                return Optional.of("Walls slow trouble. They do not stop invitations, curses, or foolish feet.");
            }
            if (barriers > 0) {
                return Optional.of("Some walling, not enough for pride. Stay near light and listen for the bell.");
            }
            return Optional.of("No proper wall nearby. Safe is a word for daylight and locked doors.");
        }
        if (containsAny(normalized, "raid", "raider", "raiders", "pillager", "pillagers", "bell")) {
            if (containsAny(normalized, "ring the bell", "if i ring", "when i ring", "what happens")) {
                return Optional.of("Ring for danger. Villagers run for doors; guards and golems look outward.");
            }
            if (facts.contains("raid active")) {
                return Optional.of("Raid is active. Door if you are soft, bow if you are useful.");
            }
            int raiders = countAfter(facts, "raiders");
            if (raiders > 0) {
                return Optional.of(countWord(raiders) + " raider" + (raiders == 1 ? "" : "s")
                        + " nearby. Not a raid yet, but bad arithmetic.");
            }
            return Optional.of("No raid on my count. Keep the bell clear anyway.");
        }
        if (containsAny(normalized, "vampire", "vampires", "blood", "sucking blood", "sucked blood")) {
            int vampires = countAfter(facts, "vampires");
            if (vampires > 0) {
                return Optional.of(countWord(vampires) + " vampire" + (vampires == 1 ? "" : "s")
                        + " nearby. Keep distance, watch the mouth, and do not invite trouble in.");
            }
            return Optional.of("No vampire on my count. I still distrust pretty manners after sunset.");
        }
        if (containsAny(normalized, "werewolf", "werewolves", "wolfman", "wolfmen", "moon")) {
            int werewolves = countAfter(facts, "werewolves");
            if (werewolves > 0) {
                return Optional.of(countWord(werewolves) + " werewolf sign" + (werewolves == 1 ? "" : "s")
                        + " nearby. Silver, distance, and no hero speeches.");
            }
            return Optional.of("No werewolf sign nearby. Moonrise can make liars of honest counts.");
        }
        if (containsAny(normalized, "witch hunter", "witch hunters", "hunter", "hunters")) {
            return Optional.of("Witch hunters are not village guards. They chase transformed folk with silver and purpose.");
        }
        if (asksGoodBadWitches(normalized)) {
            return Optional.of("Good witches leave cures and clean bargains. Bad ones leave bottles with names in them.");
        }
        if (containsAny(normalized, "witch", "witches", "coven", "curse", "taglock", "poppet")) {
            int witches = countAfter(facts, "witches");
            if (witches > 0) {
                return Optional.of("Witch sign nearby. Hands visible, hair close, and no midnight promises.");
            }
            return Optional.of("No witch close by my count. Still keep hair and names off strange hands.");
        }
        if (containsAny(normalized, "demon comes", "demon down the road", "if a demon", "demon on the road")
                || asksDemonVillagerOpinion(normalized)
                || containsAny(normalized, "demon", "demons", "devil", "hellspawn")) {
            return Optional.of("Demons get barred doors, bells, and witches who know binding. Not bargains.");
        }
        if (containsAny(normalized, "trade", "sell", "buy", "stock", "wares", "emerald")) {
            return Optional.of("I am not a merchant. I carry arrows, a blade, and poor patience.");
        }
        if (containsAny(normalized, "should i sleep", "sleep here", "sleep tonight", "stay here tonight")) {
            return Optional.of("Sleep behind a door with the bell clear. A guard is warning, not a blanket.");
        }
        if (containsAny(normalized, "what are you carrying", "your bow", "your sword", "weapon", "weapons")) {
            if (state.contains("main hand")) {
                return Optional.of("Bow for distance, sword when distance fails. That is the whole sermon.");
            }
            return Optional.of("Leather, arrows, and the stubborn hope that trouble stays at range.");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I know your name, " + name + ", and that you are still standing in my road."));
        }
        return Optional.of(NpcFallbackAsides.villageGuard(normalized, name, ""));
    }
}
