package com.emoniph.witchery.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

class WakingNightmarePlayerDataTest {
    @Test
    void curseLevelClampsAndReportsActiveState() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        assertFalse(data.hasWakingNightmare());

        data.setWakingNightmareLevel(3);
        assertEquals(3, data.getWakingNightmareLevel());
        assertTrue(data.hasWakingNightmare());

        data.setWakingNightmareLevel(-1);
        assertEquals(0, data.getWakingNightmareLevel());
        assertFalse(data.hasWakingNightmare());
    }

    @Test
    void curseStateCopiesWithPersistentPlayerData() {
        WitcheryPlayerData original = new WitcheryPlayerData();
        original.setWakingNightmareLevel(2);
        original.setOtherwhereHearthTraveler(true);

        WitcheryPlayerData copy = new WitcheryPlayerData();
        copy.copyFrom(original, false);

        assertEquals(2, copy.getWakingNightmareLevel());
        assertTrue(copy.hasWakingNightmare());
        assertTrue(copy.isOtherwhereHearthTraveler());
    }

    @Test
    void physicalSpiritTravelerFlagPersistsInNestedState() {
        WitcheryPlayerData.SpiritWorldState state = new WitcheryPlayerData.SpiritWorldState();
        state.otherwhereHearthTraveler = true;
        state.dreamStorage.overworldInventorySnbt = "{inv:[{Slot:1b}]}";
        state.dreamStorage.spiritInventorySnbt = "{inv:[{Slot:2b}]}";
        state.dreamStorage.overworldHealth = 7.0F;
        state.dreamStorage.spiritHealth = 13.0F;
        state.dreamStorage.spiritFood = 6;
        state.dreamStorage.spiritVitalsInitialized = true;

        var encoded = WitcheryPlayerData.SpiritWorldState.CODEC
                .encodeStart(NbtOps.INSTANCE, state)
                .getOrThrow();
        WitcheryPlayerData.SpiritWorldState decoded =
                WitcheryPlayerData.SpiritWorldState.CODEC
                        .parse(NbtOps.INSTANCE, encoded)
                        .getOrThrow();

        assertTrue(decoded.otherwhereHearthTraveler);
        assertEquals("{inv:[{Slot:1b}]}", decoded.dreamStorage.overworldInventorySnbt);
        assertEquals("{inv:[{Slot:2b}]}", decoded.dreamStorage.spiritInventorySnbt);
        assertEquals(7.0F, decoded.dreamStorage.overworldHealth);
        assertEquals(13.0F, decoded.dreamStorage.spiritHealth);
        assertEquals(6, decoded.dreamStorage.spiritFood);
        assertTrue(decoded.dreamStorage.spiritVitalsInitialized);
    }

    @Test
    void manifestedGhostCleanupPreservesPreparedDurationUntilItIsSpent() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setInSpiritWorld(true);
        data.setManifestedGhost(true);
        data.setManifestationTime(150);
        data.setSkipManifestationTick(true);

        assertTrue(data.isManifestedGhost());
        assertEquals(150, data.getManifestationTime());
        assertTrue(data.shouldSkipManifestationTick());

        data.clearManifestedGhostState();
        assertFalse(data.isManifestedGhost());
        assertEquals(150, data.getManifestationTime());
        assertFalse(data.shouldSkipManifestationTick());
        assertTrue(data.isInSpiritWorld());

        data.clearManifestationState();
        assertEquals(0, data.getManifestationTime());
    }
}
