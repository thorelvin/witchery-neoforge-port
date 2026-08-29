package com.emoniph.witchery.data;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchoredCurseSavedDataTest {
    private static final UUID BINDING = UUID.fromString("18fab844-66ad-454a-8628-ddcb6f7bfe7b");
    private static final UUID CASTER = UUID.fromString("ca31e204-dd18-46e3-aab5-0cfe3eb2dfea");
    private static final UUID TARGET = UUID.fromString("20caea5a-d1cb-4323-a54c-d1ab0296e274");

    @Test
    void codecPreservesIdentityRuntimeCluesAndPriorEffects() {
        AnchoredCurseSavedData.CurseRecord record = record(BINDING, new BlockPos(24, 68, -31), 2);
        AnchoredCurseSavedData original = AnchoredCurseSavedData.fromRecordsForTest(
                List.of(record), List.of(new AnchoredCurseSavedData.WardRecord(
                        AnchoredCurseSavedData.CURRENT_VERSION, CASTER, TARGET, 9_000L)));

        Tag encoded = AnchoredCurseSavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        AnchoredCurseSavedData decoded = AnchoredCurseSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        AnchoredCurseSavedData.CurseRecord restored =
                AnchoredCurseSavedData.recordsForTest(decoded).getFirst();

        assertEquals(BINDING, restored.bindingId());
        assertEquals(CASTER, restored.casterId());
        assertEquals(TARGET, restored.targetId());
        assertEquals("minecraft:overworld", restored.dimension());
        assertEquals(new BlockPos(24, 68, -31), restored.anchor());
        assertEquals(2, restored.slot());
        assertEquals(3_600, restored.activeTicks());
        assertEquals(2, restored.clueStage());
        assertEquals(Optional.of(new AnchoredCurseSavedData.EffectSnapshot(
                400, 1, false, true, true)), restored.priorSlowness());
        assertEquals(1, AnchoredCurseSavedData.wardsForTest(decoded).size());
    }

    @Test
    void duplicateCasterTargetPairAndDuplicateBindingAreRejectedOnLoad() {
        AnchoredCurseSavedData.CurseRecord first = record(BINDING, BlockPos.ZERO, 0);
        AnchoredCurseSavedData.CurseRecord duplicatePair = record(
                UUID.fromString("a4ee6bb2-4f78-4a95-b32a-f033132d15c9"), BlockPos.ZERO.east(), 1);

        AnchoredCurseSavedData decoded = AnchoredCurseSavedData.fromRecordsForTest(
                List.of(first, duplicatePair), List.of());

        assertEquals(List.of(first), AnchoredCurseSavedData.recordsForTest(decoded));
        assertTrue(decoded.hasBinding(BINDING));
    }

    @Test
    void missingRuntimeDecodesToAnInertRecord() {
        AnchoredCurseSavedData.CurseRecord record = record(BINDING, BlockPos.ZERO, 0);
        var encoded = AnchoredCurseSavedData.CurseRecord.CODEC
                .encodeStart(JsonOps.INSTANCE, record).getOrThrow().getAsJsonObject();
        encoded.remove("runtime");

        AnchoredCurseSavedData.CurseRecord decoded = AnchoredCurseSavedData.CurseRecord.CODEC
                .parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(AnchoredCurseSavedData.CurseState.ENDED, decoded.state());
        assertEquals(0, decoded.activeTicks());
        assertTrue(decoded.priorSlowness().isEmpty());
    }

    @Test
    void schedulingAndStorageBudgetsStayBounded() {
        assertEquals(20, AnchoredCurseSavedData.TICK_INTERVAL);
        assertEquals(1_200L, AnchoredCurseSavedData.WARD_TICKS);
        assertEquals(4_096, AnchoredCurseSavedData.MAX_RECORDS);
        assertTrue(AnchoredCurseSavedData.CLUE_ONE_TICKS < AnchoredCurseSavedData.CLUE_TWO_TICKS);
        assertTrue(AnchoredCurseSavedData.CLUE_TWO_TICKS < AnchoredCurseSavedData.CLUE_THREE_TICKS);
    }

    private static AnchoredCurseSavedData.CurseRecord record(UUID binding, BlockPos anchor, int slot) {
        return new AnchoredCurseSavedData.CurseRecord(
                AnchoredCurseSavedData.CURRENT_VERSION,
                binding,
                CASTER,
                "ThreadCaster",
                TARGET,
                "ThreadTarget",
                "minecraft:overworld",
                anchor,
                slot,
                AnchoredCurseSavedData.CurseType.SINKING,
                AnchoredCurseSavedData.CurseState.ACTIVE,
                1_000L,
                3_600,
                2,
                1_020L,
                4,
                3,
                Optional.of(new AnchoredCurseSavedData.EffectSnapshot(400, 1, false, true, true)),
                Optional.empty());
    }
}
