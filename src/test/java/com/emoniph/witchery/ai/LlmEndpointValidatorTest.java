package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmEndpointValidatorTest {
    @Test
    void allowsLoopbackByDefault() {
        assertTrue(LlmEndpointValidator.validate("http://127.0.0.1:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
        assertTrue(LlmEndpointValidator.validate("http://localhost:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
        assertTrue(LlmEndpointValidator.validate("http://[::1]:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
    }

    @Test
    void blocksLanByDefault() {
        LlmEndpointValidator.Validation validation =
                LlmEndpointValidator.validate("http://192.168.1.20:11434", false);

        assertFalse(validation.allowed());
    }

    @Test
    void allowsLanWhenExplicit() {
        assertTrue(LlmEndpointValidator.validate("http://192.168.1.20:11434",
                LlmEndpointValidator.Policy.PRIVATE_LAN).allowed());
        assertTrue(LlmEndpointValidator.validate("http://10.20.30.40:11434",
                LlmEndpointValidator.Policy.PRIVATE_LAN).allowed());
        assertTrue(LlmEndpointValidator.validate("http://172.31.1.2:11434",
                LlmEndpointValidator.Policy.PRIVATE_LAN).allowed());
    }

    @Test
    void rejectsDeceptiveAndPublicHostsInRestrictedPolicies() {
        assertFalse(LlmEndpointValidator.validate("http://127.attacker.example:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
        assertFalse(LlmEndpointValidator.validate("http://example.com:11434",
                LlmEndpointValidator.Policy.PRIVATE_LAN).allowed());
        assertFalse(LlmEndpointValidator.validate("http://8.8.8.8:11434",
                LlmEndpointValidator.Policy.PRIVATE_LAN).allowed());
        assertTrue(LlmEndpointValidator.validate("https://example.com/v1",
                LlmEndpointValidator.Policy.REMOTE_ALLOWED).allowed());
    }

    @Test
    void rejectsCredentialsAndMalformedLiterals() {
        assertFalse(LlmEndpointValidator.validate("http://user:secret@127.0.0.1:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
        assertFalse(LlmEndpointValidator.validate("http://127.0.0.999:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
        assertFalse(LlmEndpointValidator.validate("ftp://127.0.0.1:11434",
                LlmEndpointValidator.Policy.LOCAL_ONLY).allowed());
    }

    @Test
    void appendsProviderPathOnce() {
        assertEquals("http://127.0.0.1:11434/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434", "/api/chat").toString());
        assertEquals("http://127.0.0.1:11434/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434/api/chat", "/api/chat").toString());
    }

    @Test
    void appendsProviderPathByUriComponents() {
        assertEquals("http://127.0.0.1:11434/proxy/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434/proxy/", "api/chat").toString());
        assertEquals("http://127.0.0.1:11434/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434/api/chat?old=true#fragment",
                        "/api/chat").toString());
        assertEquals("http://127.0.0.1:11434/api/chat",
                LlmEndpointValidator.appendPath("http://127.0.0.1:11434/?old=true#fragment",
                        "/api/chat").toString());
    }
}
