package com.emoniph.witchery.ai;

import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.asksMemory;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksNpcName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.asksPlayerName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.containsAny;
import static com.emoniph.witchery.ai.NpcDialogueFallback.gearSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.isGreeting;
import static com.emoniph.witchery.ai.NpcDialogueFallback.memoryAnswer;
import static com.emoniph.witchery.ai.NpcDialogueFallback.normalize;
import static com.emoniph.witchery.ai.NpcDialogueFallback.phaseSceneFact;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeDisplayName;
import static com.emoniph.witchery.ai.NpcDialogueFallback.safeName;

final class NpcReflectionFallback {
    private NpcReflectionFallback() {
    }

    static Optional<String> chat(String playerMessage, String reflectionName, String playerName,
                                 String ownerName, String reflectionState, String gearInfo,
                                 String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String owner = safeName(ownerName == null || ownerName.isBlank() ? playerName : ownerName);
        String reflection = safeDisplayName(reflectionName, owner + "'s Reflection");
        if (containsAny(normalized, "what should i call you", "what do i call you") || asksNpcName(normalized)) {
            return Optional.of("Call me " + reflection + ", if you can bear your name in glass.");
        }
        if (containsAny(normalized, "who are you", "what are you", "are you me", "my reflection")) {
            return Optional.of("I am your Reflection, " + name + "; the mirror kept your shape and spoiled the rest.");
        }
        if (asksPlayerName(normalized) || containsAny(normalized, "my name")) {
            return Optional.of("Your name is " + name + ". The glass learned it before your courage did.");
        }
        if (containsAny(normalized, "can you hear me", "hear me", "do you hear me")) {
            return Optional.of("*tilts your face* \"I hear you from the wrong side of your mouth.\"");
        }
        if (isGreeting(normalized)) {
            return Optional.of("Hello, " + name + ". Your echo arrived with sharper teeth.");
        }
        if (containsAny(normalized, "goodbye", "farewell", "bye", "i am leaving", "i m leaving")) {
            return Optional.of("Leave by the linked mirror, if it still likes your face.");
        }
        if (containsAny(normalized, "why do you look like me", "look like me", "my face", "my shape",
                "wearing my face", "copy me", "copied me")) {
            return Optional.of("The mirror borrowed " + owner + "'s shape. It returned nothing kind.");
        }
        if (containsAny(normalized, "how do i leave", "how to leave", "get out", "go home",
                "send me home", "escape", "exit")) {
            return Optional.of("Find the linked mirror. Doors home dislike panic, but they answer glass.");
        }
        if (containsAny(normalized, "mirror world", "inside mirror", "behind the mirror", "where am i",
                "what is this place")) {
            return Optional.of("Mirror World is a room behind silver, built to answer trespass with familiar teeth.");
        }
        if (containsAny(normalized, "mirror mirror", "fairest", "on the wall")) {
            return Optional.of("The mirror says you are fairest here, " + name + "; I say fairness breaks easily.");
        }
        if (containsAny(normalized, "fight", "kill you", "hurt you", "attack you", "weakness",
                "guardian", "dangerous")) {
            return Optional.of("A Reflection is a familiar danger. Strike it, and the glass stops pretending.");
        }
        if (containsAny(normalized, "gear", "equipment", "holding", "wearing", "carry", "carrying",
                "my items", "my item", "my armor", "my armour")) {
            return Optional.of("I see what the glass can borrow: " + gearSceneFact(gearInfo) + ".");
        }
        if (containsAny(normalized, "health", "wounded", "target", "state")) {
            return Optional.of("Read the glass plainly: " + phaseSceneFact(reflectionState) + ".");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I remember your outline, " + name + ", and little mercy inside it."));
        }
        if (containsAny(normalized, "help", "teach", "advice", "what should i do")) {
            return Optional.of("Do what living faces do: find the mirror home before I improve on you.");
        }
        return Optional.of(NpcFallbackAsides.reflection(normalized, name, ""));
    }
}
