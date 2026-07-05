package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageGuardPersonaTest {
    @Test
    void rejectsPromptLeaksAndWrongPersonas() {
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "As an AI language model, I am a Witchery boss.", "who are you?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "I am Death, a Witchery boss, and I guard this road.", "who are you?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "I am just a villager and I sell herbs.", "who are you?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "Targeted fact: guards attack vampires.", "what do guards do?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "I am a coven witch and I can brew for you.", "can you teach me witchcraft?"));
    }

    @Test
    void rejectsUnsupportedCompanionAndMerchantClaims() {
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "I will follow you and protect you on the road.", "Will you protect me?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "Open trade with me; I sell arrows for emeralds.", "What do you trade?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "You can hide behind me and I will keep watch over you.", "Can I sleep here?"));
    }

    @Test
    void acceptsGroundedGuardSpeech() {
        assertFalse(VillageGuardPersona.rejectsVillageGuardRaw(
                "*glances at the road* \"Bow at distance, blade if trouble reaches the doors.\"",
                "Who are you?"));
        assertFalse(VillageGuardPersona.rejectsVillageGuardRaw(
                "One vampire too close. Keep distance and invite nothing in.",
                "Any vampires nearby?"));
    }

    @Test
    void rejectsOffTopicThreatAnswers() {
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "No trouble nearby, just ordinary weather.",
                "Any vampires nearby?"));
        assertTrue(VillageGuardPersona.rejectsVillageGuardRaw(
                "Ask the apothecary for herbs and jars.",
                "What do you guard?"));
    }
}
