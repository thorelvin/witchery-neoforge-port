package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiRequestPermitsTest {
    @Test
    void permitClosesExactlyOnce() {
        AiRequestPermits permits = new AiRequestPermits();
        AiRequestPermits.Permit permit = permits.tryAcquire(1);

        assertNotNull(permit);
        assertEquals(1, permits.inFlight());
        permit.close();
        permit.close();
        assertEquals(0, permits.inFlight());
        assertNotNull(permits.tryAcquire(1));
    }

    @Test
    void staleCompletionCannotReleaseNewGenerationPermit() {
        AiRequestPermits permits = new AiRequestPermits();
        AiRequestPermits.Permit stale = permits.tryAcquire(1);
        long oldGeneration = stale.generation();
        permits.invalidate(1);
        AiRequestPermits.Permit current = permits.tryAcquire(1);

        assertNotNull(current);
        assertNotEquals(oldGeneration, current.generation());
        stale.close();
        assertEquals(1, permits.inFlight());
        assertNull(permits.tryAcquire(1));
        current.close();
        assertEquals(0, permits.inFlight());
    }
}
