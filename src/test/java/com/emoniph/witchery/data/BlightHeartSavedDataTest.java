package com.emoniph.witchery.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlightHeartSavedDataTest {
    private static final UUID OWNER = UUID.fromString("525c3376-5db2-4243-b9f3-7d7f9c087ebf");
    private static final UUID CLEANER = UUID.fromString("30c46693-59fb-4726-94ae-32b83722e316");

    @Test
    void codecPreservesOwnerLifecycleBudgetAndExactRestorationLedger() {
        BlockPos anchor = new BlockPos(12, 63, -8);
        BlightHeartSavedData.HeartRecord record = new BlightHeartSavedData.HeartRecord(
                BlightHeartSavedData.CURRENT_VERSION,
                OWNER,
                "HeartOwner",
                "minecraft:overworld",
                anchor,
                BlightHeartSavedData.HeartState.CLEANSING,
                407,
                240L,
                Optional.of(CLEANER),
                "HeartCleaner",
                List.of(
                        new BlightHeartSavedData.Mutation(anchor,
                                Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.SCULK.defaultBlockState()),
                        new BlightHeartSavedData.Mutation(anchor.east(),
                                Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.CRIMSON_STEM.defaultBlockState())
                ));
        BlightHeartSavedData original = BlightHeartSavedData.fromRecordsForTest(List.of(record));

        Tag encoded = BlightHeartSavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        BlightHeartSavedData decoded = BlightHeartSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        BlightHeartSavedData.HeartRecord restored = BlightHeartSavedData.recordsForTest(decoded).getFirst();

        assertEquals(OWNER, restored.ownerId());
        assertEquals("HeartOwner", restored.ownerName());
        assertEquals("minecraft:overworld", restored.dimension());
        assertEquals(anchor, restored.anchor());
        assertEquals(BlightHeartSavedData.HeartState.CLEANSING, restored.state());
        assertEquals(407, restored.remainingBudget());
        assertEquals(240L, restored.nextSpreadTick());
        assertEquals(Optional.of(CLEANER), restored.cleanupActorId());
        assertEquals("HeartCleaner", restored.cleanupActorName());
        assertEquals(Blocks.GRASS_BLOCK.defaultBlockState(),
                restored.mutations().get(anchor).originalState());
        assertEquals(Blocks.CRIMSON_STEM.defaultBlockState(),
                restored.mutations().get(anchor.east()).appliedState());
    }

    @Test
    void unsupportedOrAnchorlessRecordsBecomeInertCompatibilityBlocks() {
        BlockPos anchor = BlockPos.ZERO;
        BlightHeartSavedData.HeartRecord unsupported = record(
                BlightHeartSavedData.CURRENT_VERSION + 1, anchor,
                List.of(new BlightHeartSavedData.Mutation(anchor,
                        Blocks.DIRT.defaultBlockState(), Blocks.SCULK.defaultBlockState())));
        BlightHeartSavedData.HeartRecord anchorless = record(
                BlightHeartSavedData.CURRENT_VERSION, anchor,
                List.of(new BlightHeartSavedData.Mutation(anchor.east(),
                        Blocks.STONE.defaultBlockState(), Blocks.SCULK.defaultBlockState())));

        BlightHeartSavedData decoded = BlightHeartSavedData.fromRecordsForTest(
                List.of(unsupported, anchorless));

        assertTrue(BlightHeartSavedData.recordsForTest(decoded).isEmpty());
    }

    @Test
    void persistedLedgerIsHardCapped() {
        BlockPos anchor = BlockPos.ZERO;
        List<BlightHeartSavedData.Mutation> mutations = new ArrayList<>();
        mutations.add(new BlightHeartSavedData.Mutation(anchor,
                Blocks.DIRT.defaultBlockState(), Blocks.SCULK.defaultBlockState()));
        for (int i = 1; i < BlightHeartSavedData.MAX_LEDGER_ENTRIES + 40; i++) {
            mutations.add(new BlightHeartSavedData.Mutation(new BlockPos(i, 0, 0),
                    Blocks.STONE.defaultBlockState(), Blocks.SCULK.defaultBlockState()));
        }

        BlightHeartSavedData.HeartRecord record = record(
                BlightHeartSavedData.CURRENT_VERSION, anchor, mutations);

        assertEquals(BlightHeartSavedData.MAX_LEDGER_ENTRIES, record.mutations().size());
    }

    @Test
    void lifecycleWorkBudgetsAreStructurallyBounded() {
        assertEquals(512, BlightHeartSavedData.MAX_SPREAD_MUTATIONS);
        assertEquals(20L, BlightHeartSavedData.SPREAD_INTERVAL_TICKS);
        assertEquals(4, BlightHeartSavedData.CLEANUP_HEARTS_PER_LEVEL_TICK);
        assertEquals(12, BlightHeartSavedData.CLEANUP_INSPECTIONS_PER_HEART);
        assertEquals(4, BlightHeartSavedData.CLEANUP_RESTORES_PER_HEART);
        assertTrue(BlightHeartSavedData.CLEANUP_RESTORES_PER_HEART
                <= BlightHeartSavedData.CLEANUP_INSPECTIONS_PER_HEART);
    }

    private static BlightHeartSavedData.HeartRecord record(
            int version, BlockPos anchor, List<BlightHeartSavedData.Mutation> mutations) {
        return new BlightHeartSavedData.HeartRecord(
                version,
                OWNER,
                "HeartOwner",
                "minecraft:overworld",
                anchor,
                BlightHeartSavedData.HeartState.ACTIVE,
                BlightHeartSavedData.MAX_SPREAD_MUTATIONS,
                0L,
                Optional.empty(),
                "",
                mutations);
    }
}
