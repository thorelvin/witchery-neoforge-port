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
