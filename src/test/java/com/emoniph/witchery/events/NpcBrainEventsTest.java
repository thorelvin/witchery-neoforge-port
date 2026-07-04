package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void npcChatCancellationRequiresConfigAndPrivateAddress() {
        assertTrue(NpcBrainEvents.shouldCancelHandledNpcChat(true, true));
        assertFalse(NpcBrainEvents.shouldCancelHandledNpcChat(false, true));
        assertFalse(NpcBrainEvents.shouldCancelHandledNpcChat(true, false));
    }
}
