package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmEndpointValidatorTest {
    @Test
    void allowsLoopbackByDefault() {
        assertTrue(LlmEndpointValidator.validate("http://127.0.0.1:11434", false).allowed());
        assertTrue(LlmEndpointValidator.validate("http://localhost:11434", false).allowed());
    }

    @Test
    void blocksLanByDefault() {
        LlmEndpointValidator.Validation validation =
                LlmEndpointValidator.validate("http://192.168.1.20:11434", false);

        assertFalse(validation.allowed());
    }

    @Test
    void allowsLanWhenExplicit() {
        assertTrue(LlmEndpointValidator.validate("http://192.168.1.20:11434", true).allowed());
    }

    @Test
    void appendsProviderPathOnce() {
        assertEquals("http://127.0.0.1:11434/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434", "/api/chat").toString());
        assertEquals("http://127.0.0.1:11434/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434/api/chat", "/api/chat").toString());
    }
}
