package com.emoniph.witchery.ritual;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that staged-ritual {@link RitualExecutor.RitualSavedData} survives a save/load round-trip
 * via its Codec (the persistence that lets active rites + their consumed sacrifices outlive a server
 * restart). Runs headlessly via ModDevGradle {@code unitTest}.
 */
class RitualPersistenceTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void savedDataRoundTripsStably() {
        HolderLookup.Provider provider = VanillaRegistries.createLookup();
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);

        RitualExecutor.RitualSavedData original = RitualExecutor.buildTestSavedData();

        Tag encodedOnce = RitualExecutor.RitualSavedData.CODEC.encodeStart(ops, original).getOrThrow();
        RitualExecutor.RitualSavedData decoded =
                RitualExecutor.RitualSavedData.CODEC.parse(ops, encodedOnce).getOrThrow();
        Tag encodedTwice = RitualExecutor.RitualSavedData.CODEC.encodeStart(ops, decoded).getOrThrow();

        assertEquals(1, decoded.activeRitualCount(), "the staged ritual survives the round-trip");
        assertEquals(encodedOnce, encodedTwice, "ritual SavedData re-encodes identically after a load");
    }
}
