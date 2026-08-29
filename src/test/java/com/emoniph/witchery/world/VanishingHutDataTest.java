package com.emoniph.witchery.world;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishingHutDataTest {
    private static final UUID HUT = UUID.fromString("14d2e61d-a952-462c-8b4c-1aa61d86ac91");
    private static final UUID RESIDENT = UUID.fromString("80bf0778-fc0b-420b-a6fd-1af88c095730");

    @Test
    void entryRoundTripKeepsCycleResidentBargainAndRestraint() {
        VanishingHutData.Entry original = new VanishingHutData.Entry(
                HUT, "witchery:spirit_world", new BlockPos(47, 72, -91), Optional.of(RESIDENT),
                6, true, 2, 12_345L);

        var encoded = VanishingHutData.Entry.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        VanishingHutData.Entry restored = VanishingHutData.Entry.CODEC
                .parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, restored);
        assertFalse(restored.bargainAvailable());
    }

    @Test
    void relocationAdvancesOneCycleAndClearsTransactionState() {
        VanishingHutData.Entry restrained = new VanishingHutData.Entry(
                HUT, "minecraft:overworld", BlockPos.ZERO, Optional.of(RESIDENT),
                3, true, 2, 500L);

        VanishingHutData.Entry moved = restrained.relocated(
                "witchery:spirit_world", new BlockPos(8, 90, 12), RESIDENT);

        assertEquals(4, moved.bargainCycle());
        assertTrue(moved.bargainAvailable());
        assertEquals(0, moved.restraintStage());
        assertEquals(-1L, moved.saltHeldSince());
    }

    @Test
    void removingSaltStopsTheEscapeClockButDoesNotRestoreForfeitedBargain() {
        VanishingHutData.Entry fresh = new VanishingHutData.Entry(
                HUT, "minecraft:overworld", BlockPos.ZERO, Optional.of(RESIDENT),
                0, false, 0, -1L);

        VanishingHutData.Entry released = fresh.observeSalt(200L).clearContinuousSalt();

        assertEquals(1, released.restraintStage());
        assertEquals(-1L, released.saltHeldSince());
        assertFalse(released.bargainAvailable());
    }
}
