package com.emoniph.witchery.ai;

import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.asksMemory;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksNpcName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksPlayerName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsAny;
import static com.emoniph.witchery.ai.NpcDialogueFallback.hobgoblinPatronSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.isGreeting;
import static com.emoniph.witchery.ai.NpcDialogueFallback.memoryAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.normalize;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeDisplayName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeName;

final class NpcHobgoblinFallback {
    private NpcHobgoblinFallback() {
    }

    static Optional<String> chat(String playerMessage, String hobgoblinName, String playerName,
                                 String workInfo, String patronInfo, String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String hobgoblin = safeDisplayName(hobgoblinName, "Hobgoblin");
        String work = NpcBrainResponse.sanitize(workInfo, 180);
        String patron = NpcBrainResponse.sanitize(patronInfo, 180);
        if (asksNpcName(normalized)) {
            return Optional.of("Me " + hobgoblin + ". Good nose. Better pockets.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("You " + name + ". Name not shiny, but useful.");
        }
        if (containsAny(normalized, "goodbye", "farewell", "bye")) {
            return Optional.of("Go then, " + name + ". Leave shiny. Take feet.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("*studies your coin* \"Speak quick, " + name + ". Shiny talks best.\"");
        }
        if (containsAny(normalized, "what do you trade", "what do you sell", "stock", "wares", "buy", "sell",
                "trade")) {
            return Optional.of("Coal for dust; dust and gold for nuggets; nuggets and emerald for ingot.");
        }
        if (containsAny(normalized, "koboldite", "dust", "nugget", "ingot")) {
            return Optional.of("Koboldite is best shiny. Hobgoblin dust becomes nugget, nugget becomes ingot.");
        }
        if (containsAny(normalized, "mine", "dig", "pickaxe", "leash", "work for me", "servant", "carry")) {
            if (containsAny(normalized, "forced", "force", "leashed", "leash", "servant", "slave")) {
                return Optional.of("*shrugs* \"Rope is rude. Pickaxe is ruder. Shiny pays little.\"");
            }
            String workLower = normalize(work);
            if (containsAny(workLower, "being forced", "can dig")) {
                return Optional.of("Leash is on. Pickaxe is ready. Hobgoblin digs and grumbles.");
            }
            if (containsAny(workLower, "leashed")) {
                return Optional.of("Rope is on. Give pickaxe, put chest nearby, then shiny may appear.");
            }
            return Optional.of("Leash, pickaxe, chest. Then maybe hobgoblin finds shiny dust.");
        }
        if (containsAny(normalized, "patron", "statue", "worship", "prayer", "pray", "altar")) {
            Optional<String> patronAnswer = hobgoblinPatronSceneFact(patron);
            if (patronAnswer.isPresent()) {
                return patronAnswer;
            }
            return Optional.of("Patron statue eats prayers. Witches trick it. Mog and Gulg get angry.");
        }
        if (containsAny(normalized, "mog", "gulg", "god", "gods")) {
            return Optional.of("*shrugs* \"Mog and Gulg big. Angry. Prayers keep them looking away.\"");
        }
        if (containsAny(normalized, "boss", "appeared", "spawned", "angry patron", "angry god")) {
            return Optional.of("*gestures* \"When Mog or Gulg comes, small feet run. Big trouble follows you.\"");
        }
        if (containsAny(normalized, "prank", "mischief", "mischievous", "trick me", "tricks")) {
            return Optional.of("*taps a finger* \"Hobgoblin tricks small. Bootlaces, pockets, bad luck in socks.\"");
        }
        if (containsAny(normalized, "hut", "home", "where live", "where do you live")) {
            return Optional.of("Forest hut. Small roof, bad smell, good hiding.");
        }
        if (containsAny(normalized, "villager", "villagers", "village")) {
            return Optional.of("Villager noisy. Hobgoblin no like. Too many doors.");
        }
        if (containsAny(normalized, "witchcraft", "witchery", "rite", "ritual", "brew", "cauldron",
                "circle", "coven", "demon")) {
            return Optional.of("Witch words too tall. Ask witch. Hobgoblin knows shiny.");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("Hobgoblin remembers your name, " + name + ". And pockets."));
        }
        return Optional.of(NpcFallbackAsides.hobgoblin(normalized, name, ""));
    }
}
