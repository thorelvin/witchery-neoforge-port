package com.emoniph.witchery.world;

import com.emoniph.witchery.data.WitcheryPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MirrorWorldLifecyclePersistenceTest {
    @Test
    void allocationRecordsRemainUniqueAndSurviveCodecReload() {
        MirrorRoomData original = new MirrorRoomData();
        BlockPos firstSource = new BlockPos(12, 70, -8);
        BlockPos secondSource = new BlockPos(-40, 90, 33);

        MirrorRoomData.Room first = original.getOrAllocate(Level.OVERWORLD, firstSource);
        MirrorRoomData.Room same = original.getOrAllocate(Level.OVERWORLD, firstSource);
        MirrorRoomData.Room second = original.getOrAllocate(Level.NETHER, secondSource);

        assertEquals(first, same);
        assertEquals(0, first.index());
        assertEquals(1, second.index());
        assertEquals(2, original.rooms().size());

        var encoded = MirrorRoomData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        MirrorRoomData decoded = MirrorRoomData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(2, decoded.nextRoomIndex());
        assertEquals(first, decoded.roomAtIndex(0).orElseThrow());
        assertEquals(second, decoded.roomForSource(
                Level.NETHER.identifier().toString(), secondSource).orElseThrow());
    }

    @Test
    void nestedRoutesSurviveCodecDeathCopyAndPopInLifoOrder() {
        WitcheryPlayerData original = new WitcheryPlayerData();
        BlockPos outer = new BlockPos(20, 64, 20);
        BlockPos nested = new BlockPos(9, 7, 8);
        original.pushMirrorRoute(outer, Level.OVERWORLD, 4);
        original.pushMirrorRoute(nested, Level.NETHER, 9);

        var encoded = WitcheryPlayerData.CODEC.codec().encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        WitcheryPlayerData decoded = WitcheryPlayerData.CODEC.codec()
                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(2, decoded.getMirrorRouteDepth());
        assertEquals(9, decoded.getCurrentMirrorRoute().orElseThrow().destinationRoomIndex());

        WitcheryPlayerData deathCopy = new WitcheryPlayerData();
        deathCopy.copyFrom(decoded, false);
        assertEquals(nested, deathCopy.popMirrorRoute().orElseThrow().sourceBlockPos());
        assertEquals(outer, deathCopy.popMirrorRoute().orElseThrow().sourceBlockPos());
        assertTrue(deathCopy.getCurrentMirrorRoute().isEmpty());
    }
}
