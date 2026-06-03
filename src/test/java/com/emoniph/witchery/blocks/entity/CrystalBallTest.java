package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.predictions.Predictions;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Crystal Ball's prophecy roster: every activation selects a valid, translatable
 * prediction from the weighted roster, and the roster yields varied readings rather than a single
 * stuck one. (The prophecies then come true via {@code Predictions} + {@code PredictionEvents}.)
 * Runs headlessly via ModDevGradle {@code unitTest}.
 */
class CrystalBallTest {

    @Test
    void predictionRosterAlwaysProducesAValidReading() {
        assertTrue(Predictions.rosterSize() > 0, "prediction roster must be non-empty");

        RandomSource random = RandomSource.create(0xC0FFEE);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            String key = Predictions.chooseTestKey(random);
            assertNotNull(key, "a prediction is always chosen");
            assertTrue(key.startsWith("witchery.prediction."), "valid prediction key, got: " + key);
            seen.add(key);
        }
        // The weighted selection must spread across the roster, proving the event isn't degenerate.
        assertTrue(seen.size() >= 5, "prediction event yields varied readings, got " + seen.size());
    }
}
