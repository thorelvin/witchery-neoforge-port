package com.emoniph.witchery.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class OtherwhereHearthTest {
    @Test
    void volumeUsesApprovedTwentyFiveBlockGeometry() {
        BlockPos kettle = new BlockPos(100, 64, -40);
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(kettle);

        assertEquals(new BlockPos(88, 56, -52), volume.min());
        assertEquals(new BlockPos(112, 80, -28), volume.max());
        assertEquals(15_625, volume.blockCount());
        assertTrue(volume.contains(volume.min()));
        assertTrue(volume.contains(volume.max()));
        assertFalse(volume.contains(volume.min().west()));
        assertFalse(volume.contains(volume.max().above()));
    }

    @Test
    void seerBoundaryOutlinesTheExactMovingVolume() {
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(new BlockPos(100, 64, -40));
        List<Vec3> points = OtherwhereHearthTransposition.boundaryPoints(volume);
        double minX = volume.min().getX();
        double minY = volume.min().getY();
        double minZ = volume.min().getZ();
        double maxX = volume.max().getX() + 1.0D;
        double maxY = volume.max().getY() + 1.0D;
        double maxZ = volume.max().getZ() + 1.0D;

        assertEquals(80, points.size());
        for (Vec3 point : points) {
            assertTrue(point.x >= minX && point.x <= maxX);
            assertTrue(point.y >= minY && point.y <= maxY);
            assertTrue(point.z >= minZ && point.z <= maxZ);
            int boundaryPlanes = (point.x == minX || point.x == maxX ? 1 : 0)
                    + (point.y == minY || point.y == maxY ? 1 : 0)
                    + (point.z == minZ || point.z == maxZ ? 1 : 0);
            assertTrue(boundaryPlanes >= 2, "Every particle must lie on a volume edge");
        }
    }

    @Test
    void crossingAlwaysTargetsTheOppositeApprovedDimension() {
        assertTrue(OtherwhereHearthTransposition.destinationIsSpirit(false));
        assertFalse(OtherwhereHearthTransposition.destinationIsSpirit(true));
    }

    @Test
    void waitingRecoveryDoesNotSnapResolvedTravellersBackToTheirCapturedPosition() {
        assertTrue(OtherwhereHearthTransposition.shouldReplayEntityPosition(true, true));
        assertTrue(OtherwhereHearthTransposition.shouldReplayEntityPosition(true, false));
        assertFalse(OtherwhereHearthTransposition.shouldReplayEntityPosition(false, true));
        assertTrue(OtherwhereHearthTransposition.shouldReplayEntityPosition(false, false));
    }

    @Test
    void pendingTransactionRoundTripsCompleteSnapshotsAndEntityManifest() {
        CompoundTag overworld = new CompoundTag();
        overworld.putString("marker", "waking");
        CompoundTag spirit = new CompoundTag();
        spirit.putString("marker", "spirit");
        UUID transactionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CompoundTag entitySnapshot = new CompoundTag();
        entitySnapshot.putString("id", "minecraft:armor_stand");

        OtherwhereHearthSavedData.PendingSwap pending =
                new OtherwhereHearthSavedData.PendingSwap(
                        transactionId,
                        ownerId,
                        "Morgan",
                        new BlockPos(4, 70, 9),
                        new BlockPos(-8, 62, -3),
                        new BlockPos(7, 69, 10),
                        false,
                        10_000,
                        false,
                        OtherwhereHearthSavedData.Phase.PREPARED,
                        overworld,
                        spirit,
                        List.of(new OtherwhereHearthSavedData.EntityTransfer(
                                entityId, false, true,
                                5.5D, 71.0D, 9.5D,
                                45.0F, -10.0F,
                                Optional.of(vehicleId),
                                entitySnapshot)));
        OtherwhereHearthSavedData original = new OtherwhereHearthSavedData();
        original.setPending(pending);

        overworld.putString("marker", "mutated after capture");
        entitySnapshot.putString("id", "minecraft:pig");
        Tag encoded = OtherwhereHearthSavedData.CODEC
                .encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow();
        OtherwhereHearthSavedData decoded = OtherwhereHearthSavedData.CODEC
                .parse(NbtOps.INSTANCE, encoded)
                .getOrThrow();
        OtherwhereHearthSavedData.PendingSwap restored = decoded.pending();

        assertEquals(transactionId, restored.transactionId());
        assertEquals(ownerId, restored.ownerId());
        assertEquals("Morgan", restored.ownerName());
        assertEquals("waking", restored.overworldSnapshot().getStringOr("marker", ""));
        assertEquals("spirit", restored.spiritSnapshot().getStringOr("marker", ""));
        assertEquals(10_000, restored.powerCost());
        assertFalse(restored.powerPaid());
        assertEquals(OtherwhereHearthSavedData.Phase.PREPARED, restored.phase());
        assertEquals(1, restored.entities().size());
        assertEquals(entityId, restored.entities().getFirst().entityId());
        assertEquals(Optional.of(vehicleId), restored.entities().getFirst().vehicleId());
        assertEquals("minecraft:armor_stand",
                restored.entities().getFirst().entitySnapshot().getStringOr("id", ""));
    }

    @Test
    void inspectionIsReadyOnlyWithoutBlockersAndCopiesTheDiagnosticList() {
        List<String> mutableBlockers = new java.util.ArrayList<>();
        OtherwhereHearthTransposition.Inspection ready = new OtherwhereHearthTransposition.Inspection(
                OtherwhereHearthTransposition.volumeFor(BlockPos.ZERO),
                "minecraft:overworld",
                "witchery:spirit_world",
                "none",
                true,
                true,
                "Morgan",
                true,
                1,
                2,
                6,
                new BlockPos(2, 0, 2),
                true,
                20_000,
                60_000,
                10_000,
                mutableBlockers);

        mutableBlockers.add("late mutation");
        assertTrue(ready.ready());
        assertTrue(ready.blockers().isEmpty());

        OtherwhereHearthTransposition.Inspection blocked = new OtherwhereHearthTransposition.Inspection(
                ready.volume(), ready.sourceDimension(), ready.destinationDimension(), ready.journal(),
                ready.kettlePresent(), ready.kettleBound(), ready.kettleOwner(), ready.ownerMatches(),
                ready.mediumRitualVariant(), ready.largeOtherwhereVariant(), ready.covenSize(),
                ready.altarPos(), ready.altarValid(), ready.altarPower(), ready.altarCapacity(),
                ready.powerCost(), List.of("destination occupied"));
        assertFalse(blocked.ready());
    }

    @Test
    void waitingTransitionPreservesTheCommittedChargeAndOriginalSnapshots() {
        OtherwhereHearthSavedData.PendingSwap prepared = pendingWithSinglePassenger();
        OtherwhereHearthSavedData.PendingSwap paid = prepared.withPowerPaid();
        OtherwhereHearthSavedData.PendingSwap waiting = paid.waitingForPlayers();

        assertFalse(prepared.powerPaid());
        assertTrue(paid.powerPaid());
        assertTrue(waiting.powerPaid());
        assertEquals(OtherwhereHearthSavedData.Phase.PREPARED, paid.phase());
        assertEquals(OtherwhereHearthSavedData.Phase.WAITING_FOR_PLAYERS, waiting.phase());
        assertEquals(prepared.transactionId(), waiting.transactionId());
        assertEquals(prepared.entities(), waiting.entities());
        assertEquals("waking", waiting.overworldSnapshot().getStringOr("marker", ""));
        assertEquals("spirit", waiting.spiritSnapshot().getStringOr("marker", ""));
        assertNotSame(prepared.overworldSnapshot(), waiting.overworldSnapshot());
        assertNotSame(prepared.spiritSnapshot(), waiting.spiritSnapshot());
    }

    @Test
    void legacyPreparedJournalDefaultsToUnpaidAndPrepared() {
        OtherwhereHearthSavedData.PendingSwap pending = pendingWithSinglePassenger();
        CompoundTag encoded = (CompoundTag) OtherwhereHearthSavedData.PendingSwap.CODEC
                .encodeStart(NbtOps.INSTANCE, pending)
                .getOrThrow();
        encoded.remove("power_paid");
        encoded.remove("phase");

        OtherwhereHearthSavedData.PendingSwap restored = OtherwhereHearthSavedData.PendingSwap.CODEC
                .parse(NbtOps.INSTANCE, encoded)
                .getOrThrow();
        assertFalse(restored.powerPaid());
        assertEquals(OtherwhereHearthSavedData.Phase.PREPARED, restored.phase());
        assertEquals(pending.overworldSnapshot(), restored.overworldSnapshot());
        assertEquals(pending.spiritSnapshot(), restored.spiritSnapshot());
    }

    private static OtherwhereHearthSavedData.PendingSwap pendingWithSinglePassenger() {
        CompoundTag overworld = new CompoundTag();
        overworld.putString("marker", "waking");
        CompoundTag spirit = new CompoundTag();
        spirit.putString("marker", "spirit");
        UUID vehicle = UUID.randomUUID();
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:villager");
        return new OtherwhereHearthSavedData.PendingSwap(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Morgan",
                new BlockPos(4, 70, 9),
                new BlockPos(-8, 62, -3),
                new BlockPos(7, 69, 10),
                false,
                10_000,
                false,
                OtherwhereHearthSavedData.Phase.PREPARED,
                overworld,
                spirit,
                List.of(new OtherwhereHearthSavedData.EntityTransfer(
                        UUID.randomUUID(), false, true,
                        5.5D, 71.0D, 9.5D,
                        0.0F, 0.0F,
                        Optional.of(vehicle), entity)));
    }
}
