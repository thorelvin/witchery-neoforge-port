package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlameImpPersonaTest {
    @Test
    void flameImpAllowsThreatButNotTradeOrFollow() {
        FlameImpPersona persona = NpcPersonas.FLAME_IMP;

        assertTrue(persona.allowsIntent(NpcIntent.NONE, null, null));
        assertTrue(persona.allowsIntent(NpcIntent.QUEST_HINT, null, null));
        assertTrue(persona.allowsIntent(NpcIntent.WARN_THREAT, null, null));
        assertFalse(persona.allowsIntent(NpcIntent.OPEN_TRADE, null, null));
        assertFalse(persona.allowsIntent(NpcIntent.FOLLOW_OWNER, null, null));
    }

    @Test
    void flameImpRejectsWrongPersonaAndOffTopicContractAnswers() {
        assertTrue(FlameImpPersona.rejectsFlameImpRaw("I am a full demon merchant.", "who are you?"));
        assertTrue(FlameImpPersona.rejectsFlameImpRaw("Ask the coven witch.", "will a shiny gift help?"));
        assertFalse(FlameImpPersona.rejectsFlameImpRaw("Shiny first. Nice shiny makes nicer fire.",
                "will a shiny gift help?"));
    }
}
