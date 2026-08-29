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

import java.util.List;
import java.util.UUID;

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

        assertEquals(3, decoded.activeRitualCount(), "all representative expanding rites survive the round-trip");
        assertEquals(1, decoded.sacrificeBatchCount(), "paid sacrifice escrow survives the round-trip");
        ActiveRitual fertility = decoded.activeRituals.get(0);
        ActiveRitual forestation = decoded.activeRituals.get(1);
        ActiveRitual blight = decoded.activeRituals.get(2);
        assertEquals(ActiveRitualType.FERTILITY, fertility.type);
        assertEquals(11, fertility.stage, "fertility resumes from its saved expansion ring");
        assertEquals(2, fertility.supplementalSacrifices.size(), "fertility retains its exact refund ledger");
        assertTrue(fertility.awaitingSacrifices, "per-rite sacrifice arming survives a save/load");
        assertEquals(java.util.UUID.fromString("1531d5d8-92e7-4ebf-8f3c-47c47ad36222"), fertility.ownerUuid,
                "ritual ownership survives by UUID");
        assertEquals(null, fertility.powerSourcePos, "the altar cache is reacquired after restart");
        assertEquals(ActiveRitualType.FORESTATION, forestation.type);
        assertEquals(7, forestation.stage, "forestation resumes from its saved pulse");
        assertTrue(forestation.focus.is(Items.OAK_SAPLING), "the selected sapling survives restart");
        assertEquals(new BlockPos(32, 70, -12), forestation.effectCenter,
                "the bound-waystone destination survives restart");
        assertEquals(ActiveRitualType.BLIGHT, blight.type);
        assertEquals(null, blight.ownerUuid,
                "legacy name-only ownership remains unresolved instead of transferring by name");
        assertEquals(9, blight.stage, "Blight resumes from its saved expansion ring");
        assertEquals(80, blight.maxRadius, "Blight retains its source base radius");
        assertEquals(5, blight.pulseInterval, "Blight retains its source five-tick cadence");
        assertEquals(2, blight.supplementalSacrifices.size(), "Blight retains its exact refund ledger");
        assertTrue(blight.supplementalSacrifices.getFirst().is(Items.GLISTERING_MELON_SLICE));
        SacrificeBatch batch = decoded.sacrificeBatches.getFirst();
        assertEquals(SacrificeBatchState.ARMED, batch.state);
        assertEquals(2, batch.escrow.size(), "exact paid stacks remain escrowed through restart");
        assertEquals(1, batch.visualStep, "presentation resumes at the saved per-batch step");
        assertEquals(3000, batch.altarPower, "charged altar power remains tied to the batch");
        assertEquals(encodedOnce, encodedTwice, "ritual SavedData re-encodes identically after a load");
    }

    @Test
    void everyEscrowRecoveryStateRoundTripsWithoutWorldReferences() {
        HolderLookup.Provider provider = VanillaRegistries.createLookup();
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        RitualSavedData original = new RitualSavedData();
        int index = 0;
        for (SacrificeBatchState state : SacrificeBatchState.values()) {
            BlockPos center = new BlockPos(index * 4, 70, -index * 3);
            SacrificeBatch batch = new SacrificeBatch(
                    UUID.nameUUIDFromBytes(("escrow-" + state).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    center, List.of(new net.minecraft.world.item.ItemStack(Items.EMERALD, index + 1)),
                    List.of(center.north(), center.south()), 200L + index);
            batch.state = state;
            batch.visualStep = Math.min(index, 2);
            batch.altarPos = center.east(6);
            batch.altarPower = 100 + index;
            original.sacrificeBatches.add(batch);
            index++;
        }

        RitualSavedData decoded = RitualSavedData.CODEC.parse(ops,
                RitualSavedData.CODEC.encodeStart(ops, original).getOrThrow()).getOrThrow();

        assertEquals(List.of(SacrificeBatchState.PREPARED, SacrificeBatchState.PAID,
                        SacrificeBatchState.ARMED, SacrificeBatchState.REFUNDING),
                decoded.sacrificeBatches.stream().map(batch -> batch.state).toList());
        assertEquals(List.of(1, 2, 3, 4), decoded.sacrificeBatches.stream()
                .map(batch -> batch.escrow.getFirst().getCount()).toList());
        assertTrue(java.util.Arrays.stream(SacrificeBatch.class.getDeclaredFields())
                        .noneMatch(field -> net.minecraft.world.level.Level.class.isAssignableFrom(field.getType())
                                || net.minecraft.world.entity.Entity.class.isAssignableFrom(field.getType())),
                "persisted escrow batches must not retain live levels or entities");
    }
}
