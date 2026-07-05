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
}
