package com.emoniph.witchery.world;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies a stored crystal-ball prophecy ({@link PredictionData}) survives a save/load round-trip,
 * so a player's pending prediction is not lost on logout/restart.
 */
class PredictionDataTest {

    @Test
    void activePredictionRoundTrips() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

        PredictionData data = new PredictionData();
        data.set(player, new PredictionData.Active(2 /* arrow */, 12345L));

        Tag nbt = PredictionData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        PredictionData decoded = PredictionData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();

        PredictionData.Active restored = decoded.get(player);
        assertNotNull(restored, "the stored prophecy survives the round-trip");
        assertEquals(2, restored.id(), "prophecy id preserved");
        assertEquals(12345L, restored.time(), "prophecy timestamp preserved");
    }
}
