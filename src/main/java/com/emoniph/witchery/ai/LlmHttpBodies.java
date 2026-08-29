package com.emoniph.witchery.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class LlmHttpBodies {
    private LlmHttpBodies() {
    }

    static String readBounded(InputStream input, int maxBytes) throws IOException {
        int limit = Math.clamp(maxBytes, 16_384, 1_048_576);
        try (InputStream body = input) {
            byte[] bytes = body.readNBytes(limit + 1);
            if (bytes.length > limit) {
                throw new IOException("LLM response exceeded " + limit + " bytes.");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
