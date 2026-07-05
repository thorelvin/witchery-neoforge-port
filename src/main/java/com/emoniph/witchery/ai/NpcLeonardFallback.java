package com.emoniph.witchery.ai;

import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.altarSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksMemory;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksNpcName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksPlayerName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsAny;
import static com.emoniph.witchery.ai.NpcDialogueFallback.covenSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.gearSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.isGreeting;
import static com.emoniph.witchery.ai.NpcDialogueFallback.leonardRoutedFallback;
import static com.emoniph.witchery.ai.NpcDialogueFallback.memoryAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.normalize;
import static com.emoniph.witchery.ai.NpcDialogueFallback.phaseSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeDisplayName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeName;

final class NpcLeonardFallback {
    private NpcLeonardFallback() {
    }

    static Optional<String> chat(String playerMessage, String leonardName, String playerName,
                                 String phase, String altarInfo, String covenInfo, String gearInfo,
                                 String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String leonard = safeDisplayName(leonardName, "Shade of Leonard");
        if (containsAny(normalized, "what should i call you", "what do i call you")) {
            return Optional.of("Call me " + leonard + ", if your tongue can carry the debt.");
        }
        if (asksNpcName(normalized)) {
            return Optional.of("I am " + leonard + ", the debt your cauldron dragged into shape.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + ". Names are the first coin of every bargain.");
        }
        if (containsAny(normalized, "can you hear me", "hear me", "do you hear me")) {
            return Optional.of("I hear you, " + name + ". Speak carefully; debts have good ears.");
        }
        if (containsAny(normalized, "good night", "goodbye", "farewell", "bye")) {
            return Optional.of("Go, then. Keep your blood unspent and your debts behind you.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("You stand before a debt wearing a shape, " + name + ". Speak carefully.");
        }
        if (containsAny(normalized, "demon or a goat", "demon or goat", "are you a demon", "are you goat")) {
            return Optional.of("Demon enough to collect the debt, goat-horned enough to remind you where the sabbath keeps its doors.");
        }
        if (containsAny(normalized, "where are you from", "where did you come from", "where were you summoned from",
                "summoned from", "goat legs", "goat leg", "goat demon", "goat", "hooves", "horns", "satyr",
                "sabbath")) {
            return Optional.of("The cauldron dragged my Shade from an infernal sabbath threshold, " + name
                    + "; these horns are older than your courage.");
        }
        if (containsAny(normalized, "did my cauldron create", "cauldron create", "cauldron made you",
                "cauldron make you")) {
            return Optional.of("No. Your cauldron dragged my Shade from a sabbath threshold; creation is above your station.");
        }
        if (containsAny(normalized, "how did the cauldron", "cauldron pull", "cauldron pulled",
                "why did the cauldron", "what did my altar pay", "altar pay for")) {
            return Optional.of("Your cauldron opened the threshold; the altar paid power enough to make my shadow stand.");
        }
        if (containsAny(normalized, "nether wart first", "need nether wart first")) {
            return Optional.of("Yes. Nether Wart begins the calling; skip the first debt and the threshold stays shut.");
        }
        if (containsAny(normalized, "after tear of the goddess", "after the tear", "what comes after tear",
                "what follows the tear", "follows the tear", "follows tear of the goddess")) {
            return Optional.of("Diamond Vapour follows the Tear, then Diamond, then Nether Star, before the hat seals it.");
        }
        if (containsAny(normalized, "witches hat at the end", "witch s hat at the end", "hat at the end",
                "use a witches hat", "use a witch s hat", "witches hat the last step",
                "witch s hat the last step", "hat the last step", "hat last step")) {
            return Optional.of("Yes. The Witches' Hat is the last hand on the cauldron, after the five offerings.");
        }
        if (containsAny(normalized, "how much altar power", "altar power does", "calling need",
                "power does the calling")) {
            return Optional.of("Ten thousand altar power. Less than that buys only smoke and embarrassment.");
        }
        if (containsAny(normalized, "summon", "summoning", "calling order", "call you", "called you", "bring you",
                "cauldron")) {
            return Optional.of("Nether Wart, Tear of the Goddess, Diamond Vapour, Diamond, Nether Star; then hat, cauldron, and ten thousand altar power.");
        }
        if (containsAny(normalized, "green soul", "green souls")) {
            return Optional.of("Green souls take steel; break them before they lend me their shield.");
        }
        if (containsAny(normalized, "red soul", "red souls")) {
            return Optional.of("Red souls fear fire and blast. Burn them or break them with explosions before you waste steel.");
        }
        if (containsAny(normalized, "black soul", "black souls")) {
            return Optional.of("Black souls yield to magic and potions. Bring witchcraft, not swagger.");
        }
        if (containsAny(normalized, "lost soul", "lost souls", "souls", "green soul", "red soul", "black soul")) {
            return Optional.of("Break the souls first. Green takes steel, red fears fire and blast, black yields to magic and potions.");
        }
        if (containsAny(normalized, "demonbane", "harm demons", "weakness brew", "brew weakness")) {
            return Optional.of("Demonbane bites deepest when I am already wounded. Bring it late, not proudly early.");
        }
        if (containsAny(normalized, "mortal coil", "death coil", "coil", "withering", "wither")) {
            return Optional.of("Mortal Coil is my withering mark on attackers; survive the debt or end the fight quickly.");
        }
        if (containsAny(normalized, "urn", "leonards urn", "leonard s urn", "your urn")) {
            if (containsAny(normalized, "arthana")) {
                return Optional.of("Arthana marks ritual intent; the urn stores thrown brews. Hold the tool your debt requires.");
            }
            if (containsAny(normalized, "how", "work", "works", "what does", "what do")) {
                return Optional.of("Sneak-use changes the stored brew; use releases it through the urn's shadow without spending the brew.");
            }
            if (containsAny(normalized, "spend", "consume", "uses up", "use up", "stored brew")) {
                return Optional.of("The urn releases the selected stored brew without consuming it. That is its cruelty and its gift.");
            }
            return Optional.of("The urn stores thrown brews, cycles by sneak-use, and releases the selected brew through shadow magic.");
        }
        if (containsAny(normalized, "arthana")) {
            return Optional.of("An arthana names sacrifice and precision. Against me, it is still only the hand behind it.");
        }
        if (containsAny(normalized, "mystic branch", "branch", "spell", "glyph", "symbol")) {
            return Optional.of("A branch speaks symbols. I answer with older grammar.");
        }
        if (containsAny(normalized, "altar", "altar power", "power", "nearby altar")) {
            return Optional.of("I read your altar plainly: " + altarSceneFact(altarInfo) + ".");
        }
        if (containsAny(normalized, "safe while", "safe if", "lilith", "mara")
                && containsAny(normalized, "safe", "stand here", "near me", "nearby")) {
            return Optional.of("They are witnesses, not payment. Keep steel sheathed and your witches remain beneath their own oaths.");
        }
        if (containsAny(normalized, "hate witches", "hate the witches", "hate my witches")) {
            return Optional.of("I do not waste hate on witches. They are witnesses, tools, rivals, or debts, depending on their nerve.");
        }
        if (containsAny(normalized, "coven", "witches", "nearby witches", "my coven", "coven size")) {
            if (containsAny(normalized, "danger", "safe", "helped summon", "helped", "think of")) {
                return Optional.of("Your witches are witnesses, not my flock: " + covenSceneFact(covenInfo) + ".");
            }
            return Optional.of("Your coven is no shield: " + covenSceneFact(covenInfo) + ".");
        }
        if (containsAny(normalized, "gear", "equipment", "holding", "wearing", "carry", "carrying",
                "my items", "my item", "my armor", "my armour")) {
            return Optional.of("You carry your argument openly: " + gearSceneFact(gearInfo) + ".");
        }
        if (containsAny(normalized, "bargain", "deal", "contract", "price", "debt")) {
            return Optional.of("A bargain is not a wish, " + name + ". Bring a real price, not a story.");
        }
        if (containsAny(normalized, "strike you", "hit you", "attack you", "hurt you",
                "touch you with steel", "touch you with blade", "touch you with my blade",
                "touch you with my steel")) {
            return Optional.of("Strike me and the debt becomes combat; Mortal Coil may mark attackers who mistake courage for payment.");
        }
        if (containsAny(normalized, "how do i kill you", "how can i kill you", "how to kill you",
                "how do i beat you", "weakness", "defeat you")) {
            return Optional.of("Strike by your own hand, break the souls when they rise, and save Demonbane for the wounded hour.");
        }
        if (containsAny(normalized, "don't want to fight", "dont want to fight", "do not want to fight",
                "no reason to fight", "i refuse to fight", "can we avoid fighting", "peace")) {
            return Optional.of("Then keep your hand from blood. I am neutral until struck, not gentle.");
        }
        if (containsAny(normalized, "should i fight", "fight you tonight", "fight tonight")) {
            return Optional.of("Only if you mean to pay in blood. I am neutral until struck.");
        }
        if (containsAny(normalized, "neutral until struck", "until struck")) {
            return Optional.of("Yes. I am neutral until struck; spill blood, and the debt becomes combat.");
        }
        if (containsAny(normalized, "bow", "arrow", "arrows", "projectile")) {
            return Optional.of("A bow is distance pretending to be courage. My debt answers your own hand.");
        }
        if (containsAny(normalized, "walk away", "leave now", "go away now", "if i leave")) {
            return Optional.of("Walk away while your hand is clean. The debt waits better than flesh does.");
        }
        if (containsAny(normalized, "waiting for me to make a mistake", "waiting for a mistake",
                "just waiting", "make a mistake")) {
            return Optional.of("I wait for payment or violence. Mistakes are yours to make, not mine to need.");
        }
        if (containsAny(normalized, "weakness should i remember", "what weakness should i remember",
                "remember weakness", "weakness remember")) {
            return Optional.of("Remember this: break the souls first, strike by your own hand, and save Demonbane for the wounded hour.");
        }
        if (containsAny(normalized, "phase", "health", "wounded", "fight")) {
            return Optional.of("Read the fight, " + name + ": " + phaseSceneFact(phase) + ".");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I know your name, " + name + ", and little else worth pricing."));
        }
        if (containsAny(normalized, "help", "teach", "advice", "what should i do")) {
            return Optional.of("Ask for weakness, altar, coven, gear, or the price of summoning; spare me soft wandering.");
        }
        if (containsAny(normalized, "learn from you", "teach me", "careful witch")) {
            return Optional.of("Learn warning, not comfort: circles fail, souls shield me, and every shortcut asks interest.");
        }
        if (containsAny(normalized, "final warning", "warning before i leave", "before i leave")) {
            return Optional.of("When the souls rise, break them first. When the Coil bites, end the debt quickly.");
        }
        Optional<String> routed = leonardRoutedFallback(normalized, name, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        return Optional.of(NpcFallbackAsides.leonard(normalized, name, ""));
    }
}
