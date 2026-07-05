package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcPersonaReportRegressionTest {
    @Test
    void rejectsSoftCompanionAndServantLinesFromAllPersonaSweep() {
        assertTrue(DemonPersona.rejectsDemonRaw("Farewell, my friend", "Goodbye. What should I remember?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "I am the Flame Imp, your humble servant. I serve Lord Grimorius.", "Who are you?"));
        assertTrue(ReflectionPersona.rejectsReflectionRaw(
                "You're not going to let an old friend go, are you?", "Can I trust you?"));
        assertTrue(BossPersona.rejectsBossRaw("You may leave in peace.", "Goodbye. What should I remember?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw("I will leave you alone in peace.",
                "Goodbye. What should I remember?"));

        assertFalse(DemonPersona.rejectsDemonRaw("Bring coin, courage, or a price worth smelling.",
                "What do you want?"));
        assertFalse(FlameImpPersona.rejectsFlameImpRaw("Shiny first. Nice shiny makes nicer fire.",
                "What do you want?"));
    }

    @Test
    void rejectsNarrationOnlyLinesFromAllPersonaSweep() {
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "The old witch's warning echoes in your ears as you prepare to bid farewell.",
                "Goodbye. What should I remember?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "As you depart, the Flame Imp offers a final warning in plain words.",
                "Goodbye. What should I remember?"));
        assertTrue(VillagerPersona.rejectsVillagerRaw(
                "The village's fate hangs in the balance. Your actions echo through the forest on this eve."));
        assertTrue(ReflectionPersona.rejectsReflectionRaw(
                "I understand. The mirror world guardian is displeased with your recent actions.",
                "Goodbye. What should I remember?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "The Queen of Night's gaze is cold. You feel a chill of anticipation.",
                "Can you protect me?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "The moonlight paints your silhouette against the forest floor, shadows lengthening as the night deepens.",
                "I am leaving before dawn."));
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "The air is thick with tension, and your feet feel like lead as you step into the shadows.",
                "The night feels wrong here."));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "The air grows thick with a sense of dread as Baba Yaga's voice echoes through the night.",
                "The night feels wrong here."));
        assertTrue(VillagerPersona.rejectsVillagerRaw(
                "Nightfall descends, casting shadows on our humble abode."));
    }

    @Test
    void rejectsSpeakerInversionFromAllPersonaSweep() {
        assertTrue(LeonardPersona.rejectsLeonardRaw("I'm Dev. What do you need?",
                "What should I call you?"));
        assertTrue(VillagerPersona.rejectsVillagerRaw("I'll be calling you... The Wizard Dev."));
        assertTrue(ReflectionPersona.rejectsReflectionRaw("I shall refer to myself as Dev's reflection.",
                "What should I call you?"));
    }

    @Test
    void rejectsUnsupportedProtectionAndWrongAuthorityLines() {
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "'We've got protection.' The witch steps around Dev, circling slowly.",
                "Can you protect me?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "You should consider buying the Devil's Tongue Charm to protect yourself from nightmares.",
                "Can you protect me?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "You may wish to rest in my warm embrace.", "What do you want from me?"));
        assertTrue(VillagerPersona.rejectsVillagerRawForMessage(
                "I must warn them of the danger.", "Can you protect me?"));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "I see... Well then, since you asked so nicely,", "Can you protect me?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "As a witch myself, I understand the allure of power and knowledge.",
                "What do you trade, teach, or refuse?"));
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "I am interested in your friendship and potential alliance against the evil forces.",
                "Can I trust you?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "A Flame Imp, ready to serve and satisfy. My sole purpose is to provide what you desire.",
                "Who are you?"));
        assertTrue(ReflectionPersona.rejectsReflectionRaw(
                "Your fate lies within your own hands, my reflection.",
                "Give me one warning in plain words."));
        assertTrue(BossPersona.rejectsBossRaw(
                "The night ahead will be treacherous indeed, but we shall face it together.",
                "How do I survive if this turns ugly?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "You may call me Death, your cold, ancient companion.",
                "What should I call you?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am your servant, to serve and protect.",
                "What should I call you?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Oh, my dear little one, you have brought me the Demon Heart.",
                "What happens if I bring a Demon Heart?"));
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "I am an old coven witch, dedicated to the dark arts and protecting those who seek them.",
                "What should I ask a coven witch instead?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "I am Death, the final and eternal guardian.",
                "Who are you?"));
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "LilithPoet: Contracts are sacred promises exchanged between equals.",
                "Tell me one true thing about contracts."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "The shadowy man in black stands before you, his face obscured by a dark hood.",
                "What happens if I offer a shiny gift?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am the Shadowman, protector of the dark.",
                "What should I ask a coven witch instead?"));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "There will be no more reminders of our shared past nor comfort from your kind words.",
                "What should I call you?"));
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "I am Lilith Poe, witch and guardian of this place.",
                "Who are you?"));
    }

    @Test
    void rejectsOutOfWorldMetaAndImmersionBreakingScanQuotes() {
        assertTrue(NpcBrainResponse.polishSpeech("I am Death, a Witchery boss.",
                "Death", "Dev", "What should I call you?").isEmpty());
        assertTrue(BossPersona.rejectsBossRaw(
                "I am Death, a Witchery boss final, cold, ancient, threatening never helpful in a cozy way.",
                "What do you know about demons?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "I am Death, Lord of the Undying Lands.",
                "Who are you?"));
        assertTrue(VillagerPersona.rejectsVillagerRaw(
                "I am a Witchery villager, sworn to protect our people."));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "The demon's eyes flare with intensity as he regards the player.",
                "Can I trust you?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "I am known as Hellfire or Demon of Fire by some.",
                "What should I call you?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "I am a demon, speak like the air is rent.",
                "What should I call you?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "The name means Grandmother in Russian and is used for elder women.",
                "What should I call you?"));
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "Demon Hearts are rare, valuable gems that can be traded for power and prestige.",
                "What happens if I bring a Demon Heart?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "Dev? You've just told me that I fear nightmares.",
                "What did I just tell you I fear?"));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "You can name yourself as well, human. You're not so different from me after all.",
                "I am carrying an Arthana and some salt."));
        assertTrue(ReflectionPersona.rejectsReflectionRaw(
                "Dev, I am simply a tool for observation and analysis, Dev.",
                "What should I call you?"));
    }

    @Test
    void rejectsSecondPassImmersionBreakersFromFreshReport() {
        assertTrue(CovenWitchPersona.rejectsCovenRaw(
                "LilithPoe: Yes, child, I will do what is asked of me.",
                "Can you protect me?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "I am the Demon of Discord. Your request to bargain with me is a curious one indeed.",
                "Who are you?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "You shall know me as The Demonic One.",
                "What should I call you?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "You're welcome, demon.",
                "I insulted you earlier. Should I apologize?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "You need my help, child. Just say it.",
                "The night feels wrong here."));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "I have trade goods, Dev.",
                "I am carrying an Arthana and some salt."));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "My name is Flame Imp, or as I prefer to be known by my people, Flammepoint.",
                "What should I call you?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw(
                "I'm looking for a new partner in crime.",
                "Can I trust you?"));
        assertTrue(VillagerPersona.rejectsVillagerRaw(
                "ADemonHeartisagiftfromtheDevilhimself!It'snotjustanitemofpower."));
        assertTrue(HobgoblinPersona.rejectsHobgoblinRaw(
                "The moon looms large overhead, casting long shadows across the dirt road.",
                "I am leaving before dawn."));
        assertTrue(ReflectionPersona.rejectsReflectionRaw(
                "No more contracts. The world is a game, and it's rigged against us all.",
                "Tell me one true thing about contracts."));
        assertTrue(ReflectionPersona.rejectsReflectionRaw(
                "The night air whispers coldly as it cradles you, a gentle caress from the moon.",
                "I am leaving before dawn."));
        assertTrue(LeonardPersona.rejectsLeonardRaw(
                "I am your protector and guide, Dev.",
                "Can you protect me?"));
        assertTrue(DemonPersona.rejectsDemonRaw(
                "I am the Demon who will be your guide. I will never betray your trust.",
                "Who are you?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "I will watch over you and ensure your safe passage through this world.",
                "Can I trust you?"));
        assertTrue(BossPersona.rejectsBossRaw(
                "You must prove your worth before the other gods allow you to enter their realm.",
                "What happens if I offer a shiny gift?"));
        assertTrue(BabaYagaPersona.rejectsBabaYagaRaw(
                "Do not fret, for you may find shelter in my old house tonight.",
                "You are stranger than I expected."));
    }
}
