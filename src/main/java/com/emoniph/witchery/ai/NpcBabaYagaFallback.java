package com.emoniph.witchery.ai;

import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.asksMemory;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksNpcName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksPlayerName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksRememberRequest;
import static com.emoniph.witchery.ai.NpcDialogueFallback.babaYagaRoutedFallback;
import static com.emoniph.witchery.ai.NpcDialogueFallback.bossSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsAny;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsWord;
import static com.emoniph.witchery.ai.NpcDialogueFallback.gearSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.isGreeting;
import static com.emoniph.witchery.ai.NpcDialogueFallback.memoryAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.normalize;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeDisplayName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeName;

final class NpcBabaYagaFallback {
    private NpcBabaYagaFallback() {
    }

    static Optional<String> chat(String playerMessage, String babaName, String playerName,
                                 String bossInfo, String gearInfo, String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String baba = safeDisplayName(babaName, "Baba Yaga");
        if (containsAny(normalized, "what are you", "who are you", "old witch", "crone")) {
            return Optional.of("I am " + baba + ": old teeth, old pot, and no patience for soft manners.");
        }
        if (containsAny(normalized, "what should i call you", "what do i call you") || asksNpcName(normalized)) {
            return Optional.of("Call me " + baba + ", if your teeth can keep from chattering.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + ". Names boil nicely when spoken too freely.");
        }
        if (containsAny(normalized, "do you remember my name", "remember my name")) {
            return Optional.of("Your name is " + name + ". I keep names where the pot keeps heat.");
        }
        if (containsAny(normalized, "can you hear me", "hear me", "do you hear me")) {
            return Optional.of("*sniffs* \"I hear you, " + name + ". You smell of questions and breakable skin.\"");
        }
        if (containsAny(normalized, "good night", "goodbye", "farewell", "bye")) {
            return Optional.of("Go then, " + name + ". Leave before my pot remembers your shape.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("*clicks her tongue* \"Speak, " + name + ". My broom dislikes wasted breath.\"");
        }
        if (asksRememberRequest(normalized)) {
            return Optional.of("I remember, " + name + ". I keep fears better than jars keep teeth.");
        }
        if (containsAny(normalized, "hut", "chicken legs", "forest hut", "skull fence", "skull lamp",
                "bone fence", "bones around")) {
            return Optional.of("My hut turns on chicken legs where the forest stops pretending to be kind.");
        }
        if (containsAny(normalized, "mortar", "pestle", "broom", "sweeping tracks", "fly around",
                "flying mortar")) {
            return Optional.of("Mortar carries me, pestle drives me, broom hides the road from nosy feet.");
        }
        if (containsAny(normalized, "helper or monster", "monster or helper", "good or evil",
                "are you good", "are you evil", "will you help", "can you help")) {
            return Optional.of("Helper, monster, judge, supper-cook; behave well and learn which mouth answers.");
        }
        if (containsAny(normalized, "trial", "test", "task", "impossible chore", "impossible task",
                "vasilisa", "vassilisa")) {
            return Optional.of("My lessons come as tasks with teeth. Finish them, or feed the fence.");
        }
        if (containsAny(normalized, "strike you", "hit you", "attack you", "hurt you",
                "strike with steel", "steel blade", "steel sword")) {
            return Optional.of("Proud steel pays thorns. Wood and nullifying bolts bite quieter.");
        }
        if (containsAny(normalized, "hand bow", "crossbow")) {
            return Optional.of("A hand bow is a long spoon. Load nullifying bolts if you mean to stir me.");
        }
        if (containsAny(normalized, "wooden sword", "wooden weapon", "wood weapon")) {
            return Optional.of("Ugly little wood, but useful; it slips past thorns better than proud steel.");
        }
        if (containsAny(normalized, "how do i fight", "how can i fight", "how do i beat", "how can i beat",
                "how do i kill", "weakness", "wooden", "wood ", "nullifying", "bolt", "thorns")) {
            return Optional.of("Wood and nullifying bolts bite where proud steel pays thorns; my potions punish the rest.");
        }
        if (containsAny(normalized, "damage cap", "cap damage", "too much damage", "why so little damage")) {
            return Optional.of("Big blows become little ones on my hide. Fifteen pain is plenty for one spoonful.");
        }
        if (containsAny(normalized, "magic damage", "magic hurt", "magic against you")) {
            return Optional.of("Magic nicks me poorly. Bring wit, wood, bolts, or a better funeral song.");
        }
        if (containsAny(normalized, "thorns", "reflect", "reflected", "why did i get hurt")) {
            return Optional.of("Strike me with proud flesh and thorns answer. Wooden hands are quieter.");
        }
        if (containsAny(normalized, "heal", "healing", "fire resistance", "speed", "self potion", "drink yourself")) {
            return Optional.of("I drink when fire bites, blood thins, or distance grows rude. Old witches stay inconvenient.");
        }
        if (containsAny(normalized, "drink potion", "drink potions", "drink brews", "drink them", "drink too")) {
            return Optional.of("I drink when fire bites, blood thins, or distance grows rude. Old witches stay inconvenient.");
        }
        if (containsAny(normalized, "potion", "potions", "brew", "brews", "throw", "throws")) {
            return Optional.of("Poison, blindness, slowness, weakness, wither; little bottles for loud feet.");
        }
        if (containsAny(normalized, "spider", "spiders", "animal", "animals", "pet", "pets", "undead",
                "summon", "wolf", "wolves", "dog", "dogs")) {
            return Optional.of("Send dead things or wild pets at me, and spiders may crawl home wearing their mistake.");
        }
        if (containsAny(normalized, "teleport", "blink", "vanish", "appear")) {
            return Optional.of("Run far enough and I may step closer. Old knees know crooked roads.");
        }
        if (containsAny(normalized, "sprint", "fall", "falling", "blind", "blindness")) {
            return Optional.of("Leap and run like supper, and I may put night in your eyes.");
        }
        if (containsWord(normalized, "hat")
                || containsAny(normalized, "baba hat", "babas hat", "drop", "loot", "what do you drop")) {
            return Optional.of("My hat drops when I am done needing it. Witch ingredients spill too, if you live that long.");
        }
        if (containsAny(normalized, "gear", "equipment", "holding", "wearing", "carry", "carrying",
                "my items", "my item", "my armor", "my armour")) {
            return Optional.of("I see your little tools: " + gearSceneFact(gearInfo) + ".");
        }
        if (containsAny(normalized, "owner", "owned", "despawn", "disappear", "expire")) {
            return Optional.of("Bound crones do not linger forever. Six hundred ticks, then the old road tugs.");
        }
        if (containsAny(normalized, "trade", "buy", "sell", "wares", "stock", "merchant")) {
            return Optional.of("Trade? No. I take manners, mistakes, and sometimes the rest of you.");
        }
        if (containsAny(normalized, "teach", "lesson", "learn", "advice", "what should i do")) {
            return Optional.of("Lesson one: bring wood, nullifying bolts, and manners. Forget one, and I season the pot.");
        }
        if (containsAny(normalized, "useful warning", "one warning", "give me warning", "give me a warning",
                "tell me a warning")) {
            return Optional.of("Bring wood or nullifying bolts, keep pets back, and do not sprint like supper.");
        }
        if (containsAny(normalized, "serve", "join", "protect", "help me", "guard me", "coven")) {
            return Optional.of("I serve no little coven. I may notice it, taste it, or step over it.");
        }
        if (containsAny(normalized, "boss", "health", "state", "target")) {
            return Optional.of("Look plain, " + name + ": " + bossSceneFact(bossInfo) + ".");
        }
        if (normalized.startsWith("remember ") && containsAny(normalized, "fear", "afraid", "scared")) {
            return Optional.of("I remember, " + name + ". Fear keeps the meat from wandering into every mouth.");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I remember your name, " + name + ", and your bones have not bored me yet."));
        }
        Optional<String> routed = babaYagaRoutedFallback(normalized, name, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        return Optional.of(NpcFallbackAsides.babaYaga(normalized, name, ""));
    }
}
