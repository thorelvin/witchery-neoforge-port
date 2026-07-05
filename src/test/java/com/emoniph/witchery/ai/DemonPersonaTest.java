package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemonPersonaTest {
    @Test
    void demonAllowsTradeAndThreatButNotFollow() {
        DemonPersona persona = NpcPersonas.DEMON;

        assertTrue(persona.allowsIntent(NpcIntent.NONE, null, null));
        assertTrue(persona.allowsIntent(NpcIntent.OPEN_TRADE, null, null));
        assertTrue(persona.allowsIntent(NpcIntent.WARN_THREAT, null, null));
        assertFalse(persona.allowsIntent(NpcIntent.FOLLOW_OWNER, null, null));
    }

    @Test
    void demonRejectsWrongPersonaAndOffTopicTradeAnswers() {
        assertTrue(DemonPersona.rejectsDemonRaw("I am a Flame Imp with shiny gifts.", "who are you?"));
        assertTrue(DemonPersona.rejectsDemonRaw("Ask a villager about that.", "what do you want for a bargain?"));
        assertFalse(DemonPersona.rejectsDemonRaw("Bring coin, courage, or a price worth smelling.",
                "what do you want for a bargain?"));
    }
}
