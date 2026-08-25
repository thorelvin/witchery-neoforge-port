package com.emoniph.witchery.ritual;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that staged-ritual {@link RitualSavedData} survives a save/load round-trip
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

        RitualSavedData original = RitualExecutor.buildTestSavedData();

        Tag encodedOnce = RitualSavedData.CODEC.encodeStart(ops, original).getOrThrow();
        RitualSavedData decoded =
                RitualSavedData.CODEC.parse(ops, encodedOnce).getOrThrow();
        Tag encodedTwice = RitualSavedData.CODEC.encodeStart(ops, decoded).getOrThrow();

        assertEquals(2, decoded.activeRitualCount(), "both expanding rites survive the round-trip");
        ActiveRitual fertility = decoded.activeRituals.get(0);
        ActiveRitual forestation = decoded.activeRituals.get(1);
        assertEquals(ActiveRitualType.FERTILITY, fertility.type);
        assertEquals(11, fertility.stage, "fertility resumes from its saved expansion ring");
        assertEquals(2, fertility.supplementalSacrifices.size(), "fertility retains its exact refund ledger");
        assertEquals(ActiveRitualType.FORESTATION, forestation.type);
        assertEquals(7, forestation.stage, "forestation resumes from its saved pulse");
        assertTrue(forestation.focus.is(Items.OAK_SAPLING), "the selected sapling survives restart");
        assertEquals(new BlockPos(32, 70, -12), forestation.effectCenter,
                "the bound-waystone destination survives restart");
        assertEquals(encodedOnce, encodedTwice, "ritual SavedData re-encodes identically after a load");
    }
}
