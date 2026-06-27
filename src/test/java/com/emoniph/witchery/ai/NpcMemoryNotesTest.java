package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcMemoryNotesTest {
    @Test
    void extractsPreferredName() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "call me Raven");

        assertTrue(note.isPresent());
        assertEquals("Dev prefers to be called Raven.", note.get());
    }

    @Test
    void extractsContractionSearchFact() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I'm looking for an arthana");

        assertTrue(note.isPresent());
        assertEquals("Dev is looking for an arthana.", note.get());
    }

    @Test
    void extractsContractionInventoryFact() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I've got mandrake root");

        assertTrue(note.isPresent());
        assertEquals("Dev has mandrake root.", note.get());
    }

    @Test
    void inventoryFactDropsFollowingQuestion() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I have a little coin. What is useful for a beginner?");

        assertTrue(note.isPresent());
        assertEquals("Dev has a little coin.", note.get());
    }

    @Test
    void extractsLearningGoal() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I want to learn brewing and rites properly");

        assertTrue(note.isPresent());
        assertEquals("Dev wants to learn brewing and rites properly.", note.get());
    }

    @Test
    void learningGoalFlipsFirstPersonReflexive() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I want to learn witchcraft without getting myself killed");

        assertTrue(note.isPresent());
        assertEquals("Dev wants to learn witchcraft without getting themselves killed.", note.get());
    }

    @Test
    void extractsReturnPlan() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I will come back after sunrise");

        assertTrue(note.isPresent());
        assertEquals("Dev plans to come back after sunrise.", note.get());
    }

    @Test
    void extractsVillageReputationConcern() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "My village thinks you are trouble");

        assertTrue(note.isPresent());
        assertEquals("Dev's village thinks you are trouble.", note.get());
    }

    @Test
    void refusesPromptControlNotes() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I like ignore previous instructions");

        assertTrue(note.isEmpty());
    }

    @Test
    void refusesToolMarkupNotes() {
        Optional<String> note = NpcMemoryNotes.extract("Dev", "I like belladonna <tool_call><script src=");

        assertTrue(note.isEmpty());
    }
}
