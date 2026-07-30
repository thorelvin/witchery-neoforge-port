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

        var encoded = WitcheryPlayerData.SpiritWorldState.CODEC
                .encodeStart(NbtOps.INSTANCE, state)
                .getOrThrow();
        WitcheryPlayerData.SpiritWorldState decoded =
                WitcheryPlayerData.SpiritWorldState.CODEC
                        .parse(NbtOps.INSTANCE, encoded)
                        .getOrThrow();

        assertTrue(decoded.otherwhereHearthTraveler);
    }
}
