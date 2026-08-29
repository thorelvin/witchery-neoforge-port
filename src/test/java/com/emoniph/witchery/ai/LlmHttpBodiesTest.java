package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmHttpBodiesTest {
    @Test
    void acceptsResponseExactlyAtLimit() throws IOException {
        String body = "x".repeat(16_384);

        assertEquals(body, LlmHttpBodies.readBounded(stream(body), 16_384));
    }

    @Test
    void rejectsResponseOneByteOverLimit() {
        String body = "x".repeat(16_385);

        IOException error = assertThrows(IOException.class,
                () -> LlmHttpBodies.readBounded(stream(body), 16_384));
        assertTrue(error.getMessage().contains("exceeded"));
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
