package com.emoniph.witchery.worldgen;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishingHutBargainsTest {
    private static final UUID HUT = UUID.fromString("14d2e61d-a952-462c-8b4c-1aa61d86ac91");

    @Test
    void offerIsStableForTheSameHutAndCycle() {
        assertEquals(VanishingHutBargains.forCycle(HUT, 7),
                VanishingHutBargains.forCycle(HUT, 7));
    }

    @Test
    void relocationCyclesUseTheBoundedExistingMaterialRoster() {
        EnumSet<VanishingHutBargains.Bargain> seen = EnumSet.noneOf(VanishingHutBargains.Bargain.class);
        for (int cycle = 0; cycle < 32; cycle++) {
            seen.add(VanishingHutBargains.forCycle(HUT, cycle));
        }
        assertTrue(seen.size() >= 4, "A recurring hut should expose a useful spread of bounded bargains");
        assertEquals(5, VanishingHutBargains.Bargain.values().length);
    }
}
