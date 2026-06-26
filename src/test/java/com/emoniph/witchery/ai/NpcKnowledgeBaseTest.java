package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeBaseTest {
    @Test
    void loadsMarkdownKnowledgeForAltarQuestions() {
        String context = NpcKnowledgeBase.relevantContext("how do I power an altar?", "Plains", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("altar"));
        assertTrue(lower.contains("power"));
    }

    @Test
    void skipsLoreForGenericGreeting() {
        String context = NpcKnowledgeBase.relevantContext("hi", "Plains", "day");

        assertTrue(context.isBlank());
    }

    @Test
    void retrievesItemKnowledgeForSaltQuestions() {
        String context = NpcKnowledgeBase.relevantContext("what is witches salt for?", "Forest", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("witches' salt") || lower.contains("witches salt"));
        assertTrue(lower.contains("protective") || lower.contains("cleansing"));
    }

    @Test
    void retrievesNatureKnowledgeForHerbQuestions() {
        String context = NpcKnowledgeBase.relevantContext("where do I find mandrake and belladonna?", "Forest", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("mandrake"));
        assertTrue(lower.contains("belladonna"));
    }

    @Test
    void skipsLoreForFavoriteHerbSmallTalk() {
        String context = NpcKnowledgeBase.relevantContext("what are your favorite herb?", "Forest", "day");
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what are your favorite herb?", "Forest", "day");

        assertTrue(context.isBlank());
        assertTrue(answer.isEmpty());
    }

    @Test
    void retrievesTargetedLoreForKeywordMention() {
        String context = NpcKnowledgeBase.relevantContext("spirit world sounds dangerous", "Plains", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"));
        assertTrue(lower.contains("spirit world"));
        assertTrue(lower.contains("nightmare"));
    }

    @Test
    void retrievesTargetedHerbLoreWithoutQuestionShape() {
        String context = NpcKnowledgeBase.relevantContext("belladonna then", "Plains", "day");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("targeted fact"));
        assertTrue(lower.contains("belladonna"));
        assertTrue(lower.contains("trade good") || lower.contains("ingredient"));
    }

    @Test
    void retrievesMarkdownTargetedLoreForEarlyOvenQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what does a witches oven do?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("oven"));
        assertTrue(lower.contains("fume"));
    }

    @Test
    void retrievesRumorCueForRumorQuestions() {
        String context = NpcKnowledgeBase.relevantContext("can you tell me a village rumor?", "Plains", "night");

        assertTrue(context.toLowerCase(Locale.ROOT).contains("rumor"));
    }

    @Test
    void compactAnswerUsesSpecificNamedBrewBeforeBroadSpiritLore() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("Can you brew Sleeping Brew?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("brew of sleeping"));
        assertTrue(lower.contains("not ordinary sleep"));
    }

    @Test
    void retrievesTargetedRiteLoreForRiteQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("can you teach me a rite?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("circle"));
        assertTrue(lower.contains("offerings") || lower.contains("altar power"));
    }

    @Test
    void retrievesFamiliarLoreForOwlQuestions() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("why do witches care about owls?", "Plains", "night");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("owls"));
        assertTrue(lower.contains("night") || lower.contains("flight"));
    }

    @Test
    void retrievesThreatKnowledgeForDemonQuestions() {
        String context = NpcKnowledgeBase.relevantContext("how do demons behave?", "Nether Wastes", "night");
        String lower = context.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("demon"));
        assertTrue(lower.contains("summon") || lower.contains("bargain") || lower.contains("fire"));
    }

    @Test
    void compactAnswerFindsSpecificItemNotes() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what does arthana do?", "Plains", "day");

        assertTrue(answer.isPresent());
        String lower = answer.get().toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("arthana"));
        assertTrue(lower.contains("ritual blade") || lower.contains("altar range"));
    }

    @Test
    void compactAnswerSkipsSmallTalk() {
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("hi", "Plains", "day");

        assertTrue(answer.isEmpty());
    }

    @Test
    void skipsKnowledgeForMemoryQuestions() {
        String context = NpcKnowledgeBase.relevantContext("what do you remember about me?", "Plains", "night");
        Optional<String> answer = NpcKnowledgeBase.compactAnswer("what do you remember about me?", "Plains", "night");

        assertTrue(context.isBlank());
        assertTrue(answer.isEmpty());
    }
}
