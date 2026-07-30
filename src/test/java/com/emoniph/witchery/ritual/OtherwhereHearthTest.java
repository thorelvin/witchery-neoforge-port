package com.emoniph.witchery.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
}
