package com.emoniph.witchery.world;

import com.emoniph.witchery.data.WitcheryPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TormentLifecyclePersistenceTest {
    @Test
    void dedicatedReturnAnchorAndSessionSurviveCodecAndDeathCopy() {
        BlockPos anchor = new BlockPos(43, 81, -19);
        UUID session = UUID.fromString("50ed2c65-af7d-44e4-b616-e58ee6ff26a9");
        WitcheryPlayerData original = new WitcheryPlayerData();
        original.setTormentReturn(anchor, Level.OVERWORLD, session);

        var encoded = WitcheryPlayerData.CODEC.codec().encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        WitcheryPlayerData decoded = WitcheryPlayerData.CODEC.codec()
                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(anchor, decoded.getTormentReturnPos().orElseThrow());
        assertEquals(Level.OVERWORLD.identifier().toString(), decoded.getTormentReturnDimension());
        assertEquals(session, decoded.getTormentSession().orElseThrow());

        WitcheryPlayerData deathCopy = new WitcheryPlayerData();
        deathCopy.copyFrom(decoded, false);
        assertEquals(anchor, deathCopy.getTormentReturnPos().orElseThrow());
        assertEquals(session, deathCopy.getTormentSession().orElseThrow());

        deathCopy.clearTormentReturn();
        assertTrue(deathCopy.getTormentReturnPos().isEmpty());
        assertTrue(deathCopy.getTormentSession().isEmpty());
        assertTrue(deathCopy.getTormentReturnDimension().isEmpty());
    }

    @Test
    void releasedSessionPersistsUntilEveryParticipantReconnects() {
        UUID session = UUID.fromString("3b9dad64-c482-40de-b7ed-75d7802ae791");
        UUID first = UUID.fromString("fa16ea4f-35de-45c2-86d9-a3202632e345");
        UUID second = UUID.fromString("cc6dc216-47c2-44be-a4d4-d2d4d86dc7e6");
        TormentReleaseData releases = new TormentReleaseData();
        releases.release(session, List.of(first, second));

        var encoded = TormentReleaseData.CODEC.encodeStart(NbtOps.INSTANCE, releases).getOrThrow();
        TormentReleaseData decoded = TormentReleaseData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(2, decoded.pendingPlayerCount());
        assertTrue(decoded.consume(first, session));
        assertEquals(1, decoded.pendingPlayerCount());
        assertFalse(decoded.consume(second, UUID.randomUUID()));
        assertEquals(0, decoded.pendingPlayerCount());
    }
}
