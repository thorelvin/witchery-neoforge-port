package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcBrainEventsTest {
    @Test
    void normalizeHandlesNullAndBlankInput() {
        assertEquals("", NpcBrainEvents.normalize(null));
        assertEquals("", NpcBrainEvents.normalize("   "));
    }

    @Test
    void normalizeKeepsPlainCommandWords() {
        assertEquals("mirror mirror send me home", NpcBrainEvents.normalize("Mirror, mirror: send me home!"));
    }
}
