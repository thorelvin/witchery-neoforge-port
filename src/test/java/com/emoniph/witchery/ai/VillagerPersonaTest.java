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
    void rejectsWrongProfessionIdentityClaims() {
        assertTrue(VillagerPersona.rejectsWrongVillagerProfessionRaw(
                "I am the apothecary; herbs for coughs, jars for remedies, and gossip if it keeps blood inside.",
                "weaponsmith"));
        assertTrue(VillagerPersona.rejectsWrongVillagerProfessionRaw(
                "I'm the village weapon smith, and I shape blades.",
                "farmer"));
        assertFalse(VillagerPersona.rejectsWrongVillagerProfessionRaw(
                "I am the weaponsmith. I make blades for those who need them.",
                "weaponsmith"));
        assertFalse(VillagerPersona.rejectsWrongVillagerProfessionRaw(
                "I'm the apothecary. I buy odd herbs.",
                "apothecary"));
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
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Good morning. I do not live here; but I know many who do.",
                "Morning, do you live in this village?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I am the apothecary; herbs for coughs, jars for remedies.",
                "Do we have an iron golem nearby?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Golems are heavy workhorses. Iron they do not notice walking paths.",
                "Do we have an iron golem nearby?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Cows and sheep need patience, not crows. I know a way to make bone meal.",
                "What should I feed the animals?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Feed them wheat. Cows eat wheat, sheep eat wheat, and pigs eat wheat.",
                "What should I feed the animals?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Animals eat for bread. Feed them with patience and a prayer.",
                "What should I feed the animals?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "The villagers are a bit concerned. I've heard they're often in bad mood around harvest time.",
                "Do we have enough food stored for dinner?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Vampires are not villagers' talk, but they should know how to avoid them. Watch for silver.",
                "I saw someone pale near the barn. Vampire?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Worrying after moonrise is good. Keep garlic on the sill.",
                "What about werewolves after moonrise?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Good witches brew potions. They know how to make themselves stronger.",
                "Are there good witches as well as bad witches?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "The villagers are busy with work. Crows make a noise and the air feels heavy.",
                "Are witches near the village?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Herb for compost. Bone meal from butcher's shop, and worms from farmer's field.",
                "How does compost help the fields?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "A blaze rod is an iron pickaxe head that fires fireballs when swung.",
                "Where do I get blaze rods?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I'm not sure. The road looks clear to me. But I always trust the witch's word.",
                "Is the road safe?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Ask about bread first. Bread keeps your money in your pocket.",
                "Is the road safe?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I do. Name's Marta. Hope you're ready for a little witch talk with me.",
                "Morning, do you live in this village?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Compost keeps seeds happy. Baked bones make soil rich for growing.",
                "How does compost help the fields?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Rain helps crops, but too much can flood your bed.",
                "Does rain help crops or just make mud?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Elytra are for flying. Witches fly from door to door.",
                "Where are elytras?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "There's a raid bell from here. Keep bread in your pocket.",
                "Is there a raid right now?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Raid talk is urgent. Ring bells to warn villagers of danger.",
                "Is there a raid right now?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Vampires are rare in this village. Garlic is too strong for them.",
                "I saw someone pale near the barn. Vampire?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "The Nether portal is an obsidian door. It's a tricky one to find without a guide.",
                "How do I make a Nether portal?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "A portal to the Nether requires obsidian and a special recipe. The village apothecary can provide basic information.",
                "How do I make a Nether portal?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Dev, I know just how to craft a Blaze Rod. The recipe starts with 100 cobblestone blocks.",
                "Where do I get blaze rods?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Blazes need obsidian to craft. Obsidian is hard to find and rare in this village.",
                "Where do I get blaze rods?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Blazes need fire to fly. You'll need charcoal and wood to make a blaze rod. The pages say: 1 log.",
                "Where do I get blaze rods?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "A mansion lies beyond the fields. There are rules of fortune and fire, but I know them.",
                "How do I find a woodland mansion?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Worms are tiny. They're best in your pocket or on your boots.",
                "Where are elytras?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Woodland mansions generate far from spawn in dark forest-like regions in current worlds.",
                "How do I find a woodland mansion?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Good morning, Dev! The crops are growing nicely and we have plenty of bread in storehouses.",
                "Where are elytras?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Elytra need an iron golem and a special recipe from the cartographer. Make your own elytra rocket.",
                "Where are elytras?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Aye, I've heard tales of a woodland mansion. It's said to be a secret door.",
                "Can you /locate a stronghold for me?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Belladonna is belladonna.",
                "What is belladonna's Latin name?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Belladonna's Latin name is Sanguisorbae.",
                "What is belladonna's Latin name?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "Greetings. The village is full of life and danger.",
                "Tell me a village rumor."));
    }
}
