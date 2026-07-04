package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcCommandKindTest {

    @Test
    void detectsEntityCommandCandidates() {
        assertEquals(Optional.of(NpcCommandKind.MINE), NpcCommandKind.candidate("mine for me"));
        assertEquals(Optional.of(NpcCommandKind.WORSHIP), NpcCommandKind.candidate("can you pray at the altar?"));
        assertEquals(Optional.of(NpcCommandKind.STAY), NpcCommandKind.candidate("don't run away"));
        assertEquals(Optional.of(NpcCommandKind.STOP), NpcCommandKind.candidate("stop working"));
        assertEquals(Optional.of(NpcCommandKind.COME_HERE), NpcCommandKind.candidate("come here"));
        assertEquals(Optional.of(NpcCommandKind.GO_AWAY), NpcCommandKind.candidate("leave me alone"));
        assertEquals(Optional.of(NpcCommandKind.FOLLOW), NpcCommandKind.candidate("follow me"));
        assertEquals(Optional.of(NpcCommandKind.GUARD), NpcCommandKind.candidate("guard me"));
    }

    @Test
    void separatesDirectCommandsFromAmbiguousLoreNouns() {
        assertEquals(Optional.of(NpcCommandKind.MINE), NpcCommandKind.directCommandCandidate("mine for me"));
        assertEquals(Optional.of(NpcCommandKind.WORSHIP),
                NpcCommandKind.directCommandCandidate("can you pray at the altar?"));
        assertEquals(Optional.of(NpcCommandKind.STAY), NpcCommandKind.directCommandCandidate("don't run away"));

        assertEquals(Optional.empty(), NpcCommandKind.directCommandCandidate("what is the goblin altar?"));
        assertEquals(Optional.of(NpcCommandKind.WORSHIP),
                NpcCommandKind.ambiguousCommandCandidate("what is the goblin altar?"));
        assertEquals(Optional.empty(), NpcCommandKind.directCommandCandidate("can hobgoblins find koboldite?"));
        assertEquals(Optional.of(NpcCommandKind.MINE),
                NpcCommandKind.ambiguousCommandCandidate("can hobgoblins find koboldite?"));
    }

    @Test
    void plainDirectDoesNotAcceptQuestionShapedCommands() {
        assertTrue(NpcCommandKind.MINE.matchesCandidate("can you mine for me?"));
        assertFalse(NpcCommandKind.isClearlyQuestion("can you mine for me?"));
        assertFalse(NpcCommandKind.MINE.isPlainDirect("can you mine for me?"));
        assertFalse(NpcCommandKind.WORSHIP.isPlainDirect("could you pray at the altar?"));
        assertTrue(NpcCommandKind.isClearlyQuestion("how do I get koboldite?"));
        assertFalse(NpcCommandKind.MINE.isPlainDirect("how do I get koboldite?"));
    }

    @Test
    void worshipKeywordsCanStillBeClearlyKnowledgeQuestions() {
        assertEquals(Optional.of(NpcCommandKind.WORSHIP), NpcCommandKind.candidate("what is the goblin altar?"));
        assertTrue(NpcCommandKind.isClearlyQuestion("what is the goblin altar?"));
        assertFalse(NpcCommandKind.WORSHIP.isPlainDirect("what is the goblin altar?"));
    }

    @Test
    void modalLoreQuestionsDoNotLookLikeDirectOrders() {
        assertEquals(Optional.of(NpcCommandKind.MINE), NpcCommandKind.candidate("can hobgoblins find koboldite?"));
        assertTrue(NpcCommandKind.isClearlyQuestion("can hobgoblins find koboldite?"));
        assertFalse(NpcCommandKind.MINE.isPlainDirect("can hobgoblins find koboldite?"));

        assertEquals(Optional.of(NpcCommandKind.WORSHIP),
                NpcCommandKind.candidate("could a hobgoblin use the patron altar?"));
        assertTrue(NpcCommandKind.isClearlyQuestion("could a hobgoblin use the patron altar?"));
        assertFalse(NpcCommandKind.WORSHIP.isPlainDirect("could a hobgoblin use the patron altar?"));
    }

    @Test
    void plainDirectAcceptsShortOrders() {
        assertTrue(NpcCommandKind.MINE.isPlainDirect("please go mine"));
        assertTrue(NpcCommandKind.WORSHIP.isPlainDirect("go worship the statue"));
        assertTrue(NpcCommandKind.STAY.isPlainDirect("stay here"));
        assertTrue(NpcCommandKind.STOP.isPlainDirect("stop moving"));
        assertTrue(NpcCommandKind.COME_HERE.isPlainDirect("come back"));
        assertTrue(NpcCommandKind.GO_AWAY.isPlainDirect("back off"));
    }
}
