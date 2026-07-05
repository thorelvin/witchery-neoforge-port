package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPersonaTest {
    @Test
    void bossAllowsThreatButNotTradeOrFollow() {
        BossPersona persona = NpcPersonas.BOSS;

        assertTrue(persona.allowsIntent(NpcIntent.NONE, null, null));
        assertTrue(persona.allowsIntent(NpcIntent.WARN_THREAT, null, null));
        assertTrue(persona.allowsIntent(NpcIntent.COMBAT_TAUNT, null, null));
        assertFalse(persona.allowsIntent(NpcIntent.OPEN_TRADE, null, null));
        assertFalse(persona.allowsIntent(NpcIntent.FOLLOW_OWNER, null, null));
    }

    @Test
    void bossRejectsWrongPersonaAndLeakage() {
        assertTrue(BossPersona.rejectsBossRaw("I am a coven witch and I sell fumes.", "who are you?"));
        assertTrue(BossPersona.rejectsBossRaw("The targeted fact says Death heals.", "what is Death?"));
        assertFalse(BossPersona.rejectsBossRaw("Stare carelessly and even Binky will learn your name.",
                "what happens if I stare?"));
    }
}
