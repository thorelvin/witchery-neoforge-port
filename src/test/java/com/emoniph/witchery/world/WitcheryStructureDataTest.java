package com.emoniph.witchery.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitcheryStructureDataTest {

    @Test
    void recordedStructuresRoundTripAndFindNearestVariant() {
        WitcheryStructureData data = new WitcheryStructureData();
        data.recordForTest("minecraft:overworld", WitcheryStructureData.WITCH_WELL, "regular",
                new BlockPos(100, 65, 100), 10L);
        data.recordForTest("minecraft:overworld", WitcheryStructureData.WITCH_WELL, "cave",
                new BlockPos(200, 42, 200), 20L);
        data.recordForTest("minecraft:the_nether", "witchery:warped_coven", "",
                new BlockPos(20, 70, 20), 30L);

        Tag nbt = WitcheryStructureData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        WitcheryStructureData decoded = WitcheryStructureData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();

        assertEquals(3, decoded.size());
        Optional<WitcheryStructureData.Entry> cave = decoded.nearest(
                "minecraft:overworld", WitcheryStructureData.WITCH_WELL, "cave",
                new BlockPos(0, 70, 0));

        assertTrue(cave.isPresent(), "cursed well entry survives the round-trip");
        assertEquals(new BlockPos(200, 42, 200), cave.get().pos());
    }
}
