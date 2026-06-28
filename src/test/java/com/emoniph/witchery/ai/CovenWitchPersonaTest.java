package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CovenWitchPersonaTest {
    @Test
    void classifiesHerbGiftRequests() {
        assertEquals(Optional.of(NpcSocialAction.HERB_GIFT),
                CovenWitchPersona.INSTANCE.socialAction("Can I have a herb?"));
    }

    @Test
    void classifiesInsults() {
        assertEquals(Optional.of(NpcSocialAction.INSULT),
                CovenWitchPersona.INSTANCE.socialAction("You old hag."));
        assertEquals(Optional.of(NpcSocialAction.INSULT),
                CovenWitchPersona.INSTANCE.socialAction("You fool."));
    }

    @Test
    void classifiesPraise() {
        assertEquals(Optional.of(NpcSocialAction.PRAISE),
                CovenWitchPersona.INSTANCE.socialAction("Thank you, wise witch."));
    }

    @Test
    void leavesOrdinaryKnowledgeQuestionsForConversation() {
        assertTrue(CovenWitchPersona.INSTANCE.socialAction("what is witches salt for?").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.socialAction("how do I know if a place is cursed?").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.socialAction(
                "How should I harvest mandrake without making a fool of myself?").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.socialAction(
                "I insulted a guard earlier. Should I apologize?").isEmpty());
    }

    @Test
    void routesAmbiguousInsultWordsThroughIntentCheck() {
        assertTrue(CovenWitchPersona.INSTANCE.ambiguousSocialAction(
                "How should I harvest mandrake without making a fool of myself?").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.ambiguousSocialAction(
                "That was stupid of me.").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.ambiguousSocialAction(
                "Can you fool the guards with a rite?").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.ambiguousSocialAction(
                "This answer sounds like nonsense.").isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.ambiguousSocialAction(
                "I insulted a guard earlier. Should I apologize?").isEmpty());
    }

    @Test
    void ignoresBlankMessages() {
        assertTrue(CovenWitchPersona.INSTANCE.socialAction(null).isEmpty());
        assertTrue(CovenWitchPersona.INSTANCE.socialAction("   ").isEmpty());
    }

    @Test
    void followOwnerIntentRequiresRecruitment() {
        assertTrue(CovenWitchPersona.allowsIntent(NpcIntent.NONE, false));
        assertTrue(CovenWitchPersona.allowsIntent(NpcIntent.OPEN_TRADE, false));
        assertTrue(CovenWitchPersona.allowsIntent(NpcIntent.QUEST_HINT, false));
        assertTrue(CovenWitchPersona.allowsIntent(NpcIntent.WARN_THREAT, false));
        assertTrue(CovenWitchPersona.allowsIntent(NpcIntent.COMBAT_TAUNT, false));
        assertTrue(CovenWitchPersona.allowsIntent(NpcIntent.FOLLOW_OWNER, true));
        assertFalse(CovenWitchPersona.allowsIntent(NpcIntent.FOLLOW_OWNER, false));
    }
}
