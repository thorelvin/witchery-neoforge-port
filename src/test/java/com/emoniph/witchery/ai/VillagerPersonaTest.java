package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerPersonaTest {
    @Test
    void rejectsPromptAndPersonaLeaks() {
        assertTrue(VillagerPersona.rejectsVillagerRaw("As an AI language model, I cannot see the village."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("I am the Shade of Leonard, and the player coven is weak."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("Targeted fact: server state says iron golems 1."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("I am a good witch, and I can brew for you."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("I sell fumes and make Whiff of Magic."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("I can make Mutandis if you bring coin."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("Apothecary card: knows herbs and remedies."));
        assertTrue(VillagerPersona.rejectsVillagerRaw("Good witches can help a player's coven with supplies."));
    }

    @Test
    void acceptsGroundedVillagerSpeech() {
        assertFalse(VillagerPersona.rejectsVillagerRaw(
                "*nods* \"One iron golem is nearby. Good iron, but not a miracle.\""));
    }

    @Test
    void rejectsWrongTopicOrUnperformedVillagerPromises() {
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I'd be happy to show you what I know about witchcraft. I'm the best at it.",
                "Can you teach me witchcraft?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I'll ring the bell. It'll let me know if there's a raid nearby.",
                "If I ring the bell, what happens?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "If anything comes, you can hide behind me.",
                "Goodbye, Marta. Keep your doors shut."));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I'm the apothecary. I buy odd herbs.",
                "Any guards nearby?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I could teach you some basics of witchcraft.",
                "Can you teach me witchcraft?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "The bell rings and a guard comes running. They grab you and take you away to the witch.",
                "If I ring the bell, what happens?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Yes, Dev, sleep here tonight. I'll keep watch with my taglock.",
                "Should I sleep here tonight?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "No, there are no specific witches nearby. But I'm glad you asked.",
                "Are witches near the village?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I have a Brew of Whiff of Magic, which opens doors and gates. I can sell it to you for 100 gold.",
                "Do you know Whiff of Magic?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I am a good witch, and I sell fumes to careful travelers.",
                "Are villagers afraid of witches?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Demons are just another trade of herbs and potions.",
                "What do villagers think about demons?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Apothecary. I buy odd herbs and sell what keeps frightened folk standing.",
                "How big is this village?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I know a little about werewolves, but I'd need to get my hands on some of that blood to tell you exactly.",
                "What about werewolves?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Ring the bell and you will find yourself out of place, but not in danger.",
                "If I ring the bell, what happens?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Sleeping here tonight? You're not afraid of nightfall, are you? That is not good.",
                "Should I sleep here tonight?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Apothecary, you wish to purchase a potion? You will need to make an offer that is not too steep.",
                "I want to buy something for travel."));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "The sky is the same as when we last spoke; the moon hangs high and the stars are still.",
                "How is the weather treating the village today?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Coughs and colds are common, but good nutrition means the harvest can be fine.",
                "How is the harvest looking this season?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I'm the apothecary. If you need a remedy, I can help.",
                "What did I say I fear?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "The bell jingles and rain drops hit your head as a creature moves nearby.",
                "If I ring the bell, what happens?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Ahh, a hair trade? The coven knows your hair, and I'll be happy to cut off what's needed.",
                "If a witch asks for my hair, what should I do?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Crows are weather. One crow is rain, two are gossip with wings.",
                "If the milk turns sour overnight, what do folk blame first?"));
    }
}
