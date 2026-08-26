package com.emoniph.witchery.world;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageWallDataTest {
    @Test
    void wallGateEvidenceSurvivesCodecRoundTrip() {
        BoundingBox village = new BoundingBox(-80, 50, 112, 96, 95, 288);
        VillageWallData data = new VillageWallData();
        data.markProcessed(village, 6, 3);

        Tag encoded = VillageWallData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        VillageWallData decoded = VillageWallData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertTrue(decoded.isProcessed(village));
        VillageWallData.WallRecord record = decoded.record(village).orElseThrow();
        assertEquals(6, record.gateCandidates());
        assertEquals(3, record.gates());
    }
}
